package com.pla.spiral_abyss.encounter;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

/** Native scalar save fields and the explicit encounter-health opt-out, applied before scaling. */
public final class MobSpawnData {
    public static final String SKIP_RAISING_HEALTH = "SkipRaisingHealth";
    private static final Set<String> RESERVED = Set.of("Health", "AbsorptionAmount", "DeathTime", "HurtTime",
            "HurtByTimestamp", "Age", "ForcedAge", "PersistenceRequired", "NoAI", "LeftHanded",
            "CanPickUpLoot", "FallFlying", "SleepingX", "SleepingY", "SleepingZ");

    private MobSpawnData() {}

    public static CompoundTag parse(String snbt) {
        if (snbt == null || snbt.length() > 4096) throw new JsonParseException("spawn_nbt exceeds 4096 characters");
        try {
            CompoundTag tag = TagParser.parseTag(snbt);
            if (tag.size() > 16) throw new JsonParseException("spawn_nbt allows at most 16 fields");
            for (String key : tag.getAllKeys()) {
                Tag value = tag.get(key);
                if (!key.matches("[A-Za-z][A-Za-z0-9_]{0,63}") || RESERVED.contains(key)
                        || !(value instanceof NumericTag numeric) || !Double.isFinite(numeric.getAsDouble()))
                    throw new JsonParseException("spawn_nbt requires native numeric/boolean state fields: " + key);
                if (SKIP_RAISING_HEALTH.equals(key)
                        && (!(value instanceof ByteTag flag) || flag.getAsByte() < 0 || flag.getAsByte() > 1))
                    throw new JsonParseException("SkipRaisingHealth must be 0b or 1b");
            }
            return tag;
        } catch (CommandSyntaxException ex) {
            throw new JsonParseException("Invalid spawn_nbt", ex);
        }
    }

    public static void apply(LivingEntity entity, CompoundTag overrides) {
        if (overrides.isEmpty()) return;
        CompoundTag saved = new CompoundTag();
        entity.addAdditionalSaveData(saved);
        for (String key : overrides.getAllKeys()) {
            if (SKIP_RAISING_HEALTH.equals(key)) continue;
            Tag value = overrides.get(key);
            if (value == null || !saved.contains(key, value.getId()))
                throw new IllegalArgumentException(entity.getType() + " has no matching native save field " + key);
            saved.put(key, value.copy());
        }
        entity.readAdditionalSaveData(saved);
        if (overrides.contains(SKIP_RAISING_HEALTH))
            AbyssPlatform.data(entity).putBoolean(SKIP_RAISING_HEALTH, overrides.getBoolean(SKIP_RAISING_HEALTH));
        entity.refreshDimensions();
    }
}
