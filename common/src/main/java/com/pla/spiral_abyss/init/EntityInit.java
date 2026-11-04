package com.pla.spiral_abyss.init;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;

import com.pla.spiral_abyss.SpiralAbyss;
import com.pla.spiral_abyss.entity.KeyEntity;
import com.pla.spiral_abyss.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.pla.spiral_abyss.platform.AbyssRegistries;

public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(SpiralAbyss.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<KeyEntity>> KEY_ENTITY =
            ENTITY_TYPES.register("key_entity", () -> EntityType.Builder.of(KeyEntity::new, MobCategory.CREATURE)
                    .sized(2.5f, 4.0f)
                    .build("key_entity"));

    public static final RegistrySupplier<EntityType<CopperChestEntity>> COPPER_CHEST =
            ENTITY_TYPES.register("copper_chest", () -> EntityType.Builder.of(CopperChestEntity::new, MobCategory.MISC)
                    .sized(1.3f, 1.0f).build("copper_chest"));

    public static final RegistrySupplier<EntityType<IronChestEntity>> IRON_CHEST =
            ENTITY_TYPES.register("iron_chest", () -> EntityType.Builder.of(IronChestEntity::new, MobCategory.MISC)
                    .sized(1.3f, 1.0f).build("iron_chest"));

    public static final RegistrySupplier<EntityType<GoldenChestEntity>> GOLDEN_CHEST =
            ENTITY_TYPES.register("golden_chest", () -> EntityType.Builder.of(GoldenChestEntity::new, MobCategory.MISC)
                    .sized(1.3f, 1.0f).build("golden_chest"));

    public static final RegistrySupplier<EntityType<ExperienceChestEntity>> EXPERIENCE_CHEST =
            ENTITY_TYPES.register("experience_chest", () -> EntityType.Builder.of(ExperienceChestEntity::new, MobCategory.MISC)
                    .sized(1.3f, 1.0f).build("experience_chest"));

    public static final RegistrySupplier<EntityType<ChestLootEntity>> CHEST_LOOT =
            ENTITY_TYPES.register("chest_loot", () -> EntityType.Builder.<ChestLootEntity>of(ChestLootEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(1).build("chest_loot"));


}
