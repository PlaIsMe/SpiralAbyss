package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.config.AbyssConfig;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.world.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Per-attacker boss protection. No client flag participates in damage authorization. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssRejection {
    public static final float DAMAGE_MULTIPLIER = 0.4F;
    private AbyssRejection() {}

    public static boolean isRejected(ServerPlayer player, int floor, ResourceLocation id) {
        return player != null && AbyssConfig.ABYSSAL_REJECTION.get() && (floor == 5 || floor == 6)
                && MobCatalog.isBoss(id) && !BossProgress.hasDefeated(player, id);
    }
    public static boolean applies(ServerPlayer player, LivingEntity victim) {
        if (player == null || !WaveAllies.waveMob(victim) || !victim.getPersistentData().hasUUID("spiral_abyss_host")) return false;
        var session = AbyssParties.get(player.server).hostedBy(victim.getPersistentData().getUUID("spiral_abyss_host"));
        // Resolve the victim's attempt: lingering owned damage retains the penalty after defeat or departure.
        return session != null && session.controller().owns(victim)
                && isRejected(player, session.controller().currentFloor(), bossId(victim));
    }
    public static ResourceLocation bossId(LivingEntity victim) {
        var root = ResourceLocation.tryParse(victim.getPersistentData().getString(EncounterReinforcements.PHASE_BOSS));
        return root != null && MobCatalog.isBoss(root) ? root : ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
    }
    public static ServerPlayer attacker(DamageSource source) {
        ServerPlayer player = playerOwner(source.getEntity());
        return player != null ? player : playerOwner(source.getDirectEntity());
    }
    private static ServerPlayer playerOwner(Entity entity) {
        for (int depth = 0; entity != null && depth < 8; depth++) {
            if (entity instanceof ServerPlayer player) return player;
            Entity next = WaveAllies.owner(entity);
            if (next == entity && entity instanceof OwnableEntity owned) next = owned.getOwner();
            if (next == entity) return null;
            entity = next;
        }
        return null;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void scaleDamage(LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && applies(attacker(event.getSource()), event.getEntity()))
            event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
    }
}
