package com.pla.spiral_abyss.menu;

import com.pla.spiral_abyss.encounter.ChestPayout;
import com.pla.spiral_abyss.event.RareRewardBroadcast;
import com.pla.spiral_abyss.init.MenuInit;
import com.pla.spiral_abyss.world.ArenaSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** A shared, paged view of the saved payout. Taking transfers directly to inventory, never to a loose drop. */
public final class RewardChestMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 54;
    private final SimpleContainer view = new SimpleContainer(PAGE_SIZE);
    private final ContainerData data = new SimpleContainerData(PAGE_SIZE + 2);
    private final ArenaSession session;
    private final ChestPayout payout;
    private final int tier;

    public RewardChestMenu(int id, Inventory inventory) { this(id, inventory, null, null, 0); }
    public RewardChestMenu(int id, Inventory inventory, ArenaSession session, ChestPayout payout, int tier) {
        super(MenuInit.REWARD_CHEST.get(), id);
        this.session = session; this.payout = payout; this.tier = tier;
        for (int i = 0; i < PAGE_SIZE; i++) addSlot(new Slot(view, i, 8 + i % 9 * 18, 28 + i / 9 * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 158 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 216));
        addDataSlots(data);
        refresh();
    }
    public int page() { return data.get(PAGE_SIZE); }
    public int pages() { return Math.max(1, data.get(PAGE_SIZE + 1)); }
    public boolean rare(int slot) { return slot >= 0 && slot < PAGE_SIZE && data.get(slot) != 0; }
    private void refresh() {
        if (payout == null) return;
        int count = payout.pending() == null ? 0 : payout.pending().items().size();
        data.set(PAGE_SIZE + 1, Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE));
        data.set(PAGE_SIZE, Math.min(page(), pages() - 1));
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page() * PAGE_SIZE + i;
            view.setItem(i, payout.item(index));
            data.set(i, payout.pending() != null && payout.pending().rare(index) && !view.getItem(i).isEmpty() ? 1 : 0);
        }
    }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) {
        return session != null && player instanceof ServerPlayer serverPlayer
                && session.canTakeReward(serverPlayer, payout, tier);
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || button < 0 || button > 1) return false;
        data.set(PAGE_SIZE, Math.max(0, Math.min(pages() - 1, page() + (button == 0 ? -1 : 1))));
        broadcastChanges(); return true;
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        if (slot >= 0 && slot < PAGE_SIZE && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE)) {
            if (button < 0 || button > 1) return;
            int index = page() * PAGE_SIZE + slot;
            ItemStack saved = payout.item(index);
            if (saved.isEmpty()) return;
            ItemStack transfer = saved.copy();
            if (type == ClickType.PICKUP && button == 1) transfer.setCount(1);
            int requested = transfer.getCount();
            ItemStack acquired = transfer.copy();
            boolean rare = payout.pending().rare(index);
            player.getInventory().add(transfer);
            int taken = requested - transfer.getCount();
            if (taken > 0) {
                saved.shrink(taken);
                payout.remaining(index, saved);
                if (rare) RareRewardBroadcast.acquired((ServerPlayer) player, acquired, session.controller().currentFloor());
                session.finishContainerPayout(payout, tier);
            }
            broadcastChanges();
        }
        // Inventory is shown for capacity. Deposits, swaps, throws and cursor operations are intentionally unavailable.
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
