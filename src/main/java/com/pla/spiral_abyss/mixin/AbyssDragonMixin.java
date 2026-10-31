package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.boss_enviroment.BossEnvironments;
import com.pla.spiral_abyss.encounter.DragonArena;
import com.pla.spiral_abyss.encounter.DragonCombat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.*;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;

@Mixin(EnderDragon.class)
public abstract class AbyssDragonMixin {
    @Shadow private EndDragonFight dragonFight;
    @Shadow private BlockPos fightOrigin;
    @Shadow @Final private Node[] nodes;
    @Shadow @Final private int[] nodeAdjacency;
    @Unique private final DragonCombat spiralAbyss$combat = new DragonCombat();

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void spiralAbyss$combatPhases(CallbackInfo ci) {
        spiralAbyss$combat.tick((EnderDragon) (Object) this);
    }
    @Unique private static final EntityDataAccessor<BlockPos> SPIRAL_ABYSS_ORIGIN = SynchedEntityData.defineId(EnderDragon.class, EntityDataSerializers.BLOCK_POS);
    @Unique private static final EntityDataAccessor<Optional<UUID>> SPIRAL_ABYSS_CRYSTAL = SynchedEntityData.defineId(EnderDragon.class, EntityDataSerializers.OPTIONAL_UUID);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void spiralAbyss$defineData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon) (Object) this;
        builder.define(SPIRAL_ABYSS_ORIGIN, BlockPos.ZERO);
        builder.define(SPIRAL_ABYSS_CRYSTAL, Optional.empty());
    }
    @Inject(method = "setFightOrigin", at = @At("TAIL"))
    private void spiralAbyss$syncOrigin(BlockPos origin, CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon) (Object) this;
        if (DragonArena.abyss(dragon) && !dragon.level().isClientSide) dragon.getEntityData().set(SPIRAL_ABYSS_ORIGIN, origin);
    }
    @Inject(method = "onSyncedDataUpdated", at = @At("HEAD"))
    private void spiralAbyss$receiveOrigin(EntityDataAccessor<?> key, CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon) (Object) this;
        if (key.equals(SPIRAL_ABYSS_ORIGIN) && DragonArena.abyss(dragon)) fightOrigin = dragon.getEntityData().get(SPIRAL_ABYSS_ORIGIN);
        if (key.equals(SPIRAL_ABYSS_CRYSTAL) && DragonArena.abyss(dragon) && dragon.getEntityData().get(SPIRAL_ABYSS_CRYSTAL).isEmpty()) dragon.nearestCrystal = null;
    }
    @Inject(method = {"aiStep", "tickDeath", "kill"}, at = @At("HEAD"))
    private void spiralAbyss$detachEndFight(CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon) (Object) this;
        if (!DragonArena.abyss(dragon)) return;
        dragonFight = null;
        if (DragonArena.defeated(dragon)) {
            dragon.nearestCrystal = null;
            if (!dragon.level().isClientSide) dragon.getEntityData().set(SPIRAL_ABYSS_CRYSTAL, Optional.empty());
        }
        if (DragonArena.managed(dragon)) dragon.setFightOrigin(BlockPos.of(dragon.getPersistentData().getLong(DragonArena.ORIGIN)));
    }
    @Inject(method = "setDragonFight", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$noEndFight(EndDragonFight fight, CallbackInfo ci) {
        if (DragonArena.abyss((EnderDragon) (Object) this)) { dragonFight = null; ci.cancel(); }
    }
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getDragonFight()Lnet/minecraft/world/level/dimension/end/EndDragonFight;"))
    private EndDragonFight spiralAbyss$noAutomaticFight(ServerLevel level) {
        return DragonArena.abyss((EnderDragon) (Object) this) ? null : level.getDragonFight();
    }
    @Inject(method = "findClosestNode()I", at = @At("HEAD"))
    private void spiralAbyss$localNodes(CallbackInfoReturnable<Integer> ci) {
        EnderDragon dragon = (EnderDragon) (Object) this;
        if (!DragonArena.managed(dragon) || nodes[0] != null) return;
        for (int index = 0; index < nodes.length; index++) {
            BlockPos pos = DragonArena.node(fightOrigin, index);
            nodes[index] = new Node(pos.getX(), pos.getY(), pos.getZ());
        }
        // Vanilla ring topology, applied to arena-relative nodes without querying chunks at 0,0.
        int[] graph = {6146,8197,8202,16404,32808,32848,65696,131392,131712,263424,526848,525313,1581057,3166214,2138120,6373424,4358208,12910976,9044480,9706496,15216640,13688832,11763712,8257536};
        System.arraycopy(graph, 0, nodeAdjacency, 0, graph.length);
    }
    @Inject(method = "checkCrystals", at = @At("HEAD"), cancellable = true)
    private void spiralAbyss$ownedCrystals(CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon) (Object) this;
        if (!DragonArena.abyss(dragon)) return;
        if (DragonArena.defeated(dragon)) {
            dragon.nearestCrystal = null;
            if (!dragon.level().isClientSide) dragon.getEntityData().set(SPIRAL_ABYSS_CRYSTAL, Optional.empty());
            ci.cancel();
            return;
        }
        if (dragon.level().isClientSide) {
            Optional<UUID> selected = dragon.getEntityData().get(SPIRAL_ABYSS_CRYSTAL);
            dragon.nearestCrystal = selected.flatMap(id -> dragon.level().getEntitiesOfClass(EndCrystal.class, dragon.getBoundingBox().inflate(96), crystal -> crystal.getUUID().equals(id)).stream().findFirst()).orElse(null);
        } else {
            dragon.nearestCrystal = dragon.level().getEntitiesOfClass(EndCrystal.class, dragon.getBoundingBox().inflate(64), crystal -> BossEnvironments.crystalFor(crystal, dragon)).stream().min(Comparator.comparingDouble(dragon::distanceToSqr)).orElse(null);
            if (dragon.nearestCrystal != null && dragon.tickCount % 10 == 0 && dragon.getHealth() < dragon.getMaxHealth()) dragon.setHealth(Math.min(dragon.getMaxHealth(), dragon.getHealth() + 1));
            dragon.getEntityData().set(SPIRAL_ABYSS_CRYSTAL, Optional.ofNullable(dragon.nearestCrystal).map(EndCrystal::getUUID));
        }
        ci.cancel();
    }
    @Redirect(method = "tickDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private void spiralAbyss$noDragonXp(ServerLevel level, Vec3 position, int amount) {
        if (!DragonArena.abyss((EnderDragon) (Object) this)) ExperienceOrb.award(level, position, amount);
    }
}
