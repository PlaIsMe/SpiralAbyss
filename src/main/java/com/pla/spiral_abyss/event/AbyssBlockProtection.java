package com.pla.spiral_abyss.event;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.init.BlockInit;
import com.pla.spiral_abyss.world.AbyssDimension;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Protects permanent architecture and prevents non-creative building inside the Abyss. */
@Mod.EventBusSubscriber(modid = SpiralAbyss.MOD_ID)
public final class AbyssBlockProtection {
    private AbyssBlockProtection() {}

    @SubscribeEvent
    public static void preventBreaking(BlockEvent.BreakEvent event) {
        if (!event.getPlayer().isCreative()
                && BlockInit.isAbyssArchitecture(event.getState().getBlock())) event.setCanceled(true);
    }

    private static boolean restricted(Player player) {
        return !player.isCreative() && player.level().dimension().equals(AbyssDimension.LEVEL);
    }

    @SubscribeEvent
    public static void preventPlacement(BlockEvent.EntityPlaceEvent event) {
        // EntityMultiPlaceEvent inherits this event, covering beds and other multi-block placements.
        if (event.getEntity() instanceof Player player && restricted(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void preventBlockItemUse(PlayerInteractEvent.RightClickBlock event) {
        if (restricted(event.getEntity()) && (event.getItemStack().getItem() instanceof BlockItem
                || event.getItemStack().getItem() instanceof BucketItem)) {
            event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventBucketUse(PlayerInteractEvent.RightClickItem event) {
        if (restricted(event.getEntity()) && event.getItemStack().getItem() instanceof BucketItem) {
            event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventBucketFill(FillBucketEvent event) {
        if (restricted(event.getEntity())) event.setCanceled(true);
    }
}
