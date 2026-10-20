package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.network.AbyssNetwork.BannerBoss;
import com.pla.spiral_abyss.network.AbyssNetwork.RewardPreview;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Read-only journal data from the same persisted selections and linked rewards used by combat. */
public final class JournalPreviews {
    private JournalPreviews() {}

    public static List<BannerBoss> bosses(ServerPlayer player) {
        var parties = AbyssParties.get(player.server);
        var rewards = new RewardPreviews(player.server);
        var noBaseRewards = new FloorDefinition.Rewards(List.of(), 0, 0);
        List<BannerBoss> result = new ArrayList<>();
        for (int floor = 5; floor <= 6; floor++) {
            var definition = FloorCatalog.snapshot().floors().get(floor);
            if (definition == null) continue;
            for (int wave = floor == 5 ? 3 : 2; wave <= 3; wave++) {
                if (definition.waves().size() < wave) continue;
                try {
                    for (var enemy : parties.cycleLineup(floor, wave, definition.waves().get(wave - 1))) {
                        if (enemy.category() != MobCategory.BOSS && enemy.category() != null) continue;
                        List<RewardPreview> linked = rewards.floor(noBaseRewards, Set.of(enemy.id())).stream()
                                .filter(reward -> reward.rare() && !reward.table()).toList();
                        // Preserve separate final-wave slots even if a small installed pool repeats a species.
                        for (int count = 0; count < enemy.count() && result.size() < 4; count++)
                            result.add(new BannerBoss(floor, wave, enemy.id(), linked,
                                    MobCatalog.bossOptions(enemy.id()).previewAnimation()));
                    }
                } catch (IllegalArgumentException ex) {
                    com.mojang.logging.LogUtils.getLogger().warn("Cannot preview Abyss journal floor {} wave {}: {}", floor, wave, ex.getMessage());
                }
            }
        }
        return List.copyOf(result);
    }
}
