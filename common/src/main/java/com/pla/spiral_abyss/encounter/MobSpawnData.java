package com.pla.spiral_abyss.encounter;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

/** Verified scalar native save fields, applied after finalizeSpawn and before encounter scaling. */
public final class MobSpawnData {
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
            Tag value = overrides.get(key);
            if (value == null || !saved.contains(key, value.getId()))
                throw new IllegalArgumentException(entity.getType() + " has no matching native save field " + key);
            saved.put(key, value.copy());
        }
        entity.readAdditionalSaveData(saved);
        entity.refreshDimensions();
    }
}
