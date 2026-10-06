package com.pla.spiral_abyss.boss_enviroment;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Registry-ID dispatch shared by vanilla handlers and future datapack/mod environments. */
public final class BossEnvironmentRegistry {
    public interface Definition {
        ResourceLocation entityId();
        BossEnvironmentCatalog.Profile profile();
    }
    private static final Map<ResourceLocation, Definition> definitions = new LinkedHashMap<>();
    static { register(new WitherEnv()); register(new EnderDragonEnv()); }
    private BossEnvironmentRegistry() {}
    public static synchronized void register(Definition definition) {
        if (definitions.putIfAbsent(definition.entityId(), definition) != null) throw new IllegalArgumentException("Duplicate boss environment " + definition.entityId());
    }
    public static synchronized Map<ResourceLocation, BossEnvironmentCatalog.Profile> defaults() {
        Map<ResourceLocation, BossEnvironmentCatalog.Profile> result = new LinkedHashMap<>();
        definitions.forEach((id, definition) -> result.put(id, definition.profile()));
        return result;
    }
    public static BossEnvironmentCatalog.Profile forMob(ResourceLocation id) { return BossEnvironmentCatalog.get(id); }
}
