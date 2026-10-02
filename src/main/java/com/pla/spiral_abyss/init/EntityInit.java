package com.pla.spiral_abyss.init;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.entity.KeyEntity;
import com.pla.spiral_abyss.entity.RewardChestEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SpiralAbyss.MOD_ID);

    public static final RegistryObject<EntityType<KeyEntity>> KEY_ENTITY =
            ENTITY_TYPES.register("key_entity", () -> EntityType.Builder.of(KeyEntity::new, MobCategory.CREATURE)
                    .sized(2.5f, 4.0f)
                    .build("key_entity"));

    public static final RegistryObject<EntityType<RewardChestEntity>> REWARD_CHEST =
            ENTITY_TYPES.register("reward_chest", () -> EntityType.Builder.of(RewardChestEntity::new, MobCategory.MISC)
                    .sized(1.8f, 1.4f).build("reward_chest"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
