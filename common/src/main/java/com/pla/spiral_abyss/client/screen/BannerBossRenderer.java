package com.pla.spiral_abyss.client.screen;

import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.BossOptions.PreviewAnimation;
import com.pla.spiral_abyss.encounter.MobSpawnData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.warden.Warden;
import org.joml.Quaternionf;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.pla.spiral_abyss.client.screen.BannerPortrait.Crop;
import com.pla.spiral_abyss.client.screen.BannerPortrait.RenderState;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/** Screen-owned, unspawned models sampled once; never run entity/AI ticks to obtain a pose. */
final class BannerBossRenderer {
    private static final TagKey<EntityType<?>> PREVIEW_UPRIGHT_NORMALS = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("spiral_abyss", "preview_upright_normals"));
    private static final TagKey<EntityType<?>> PREVIEW_ADULT_AGE_TICKS = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("spiral_abyss", "preview_adult_age_ticks"));
    private static final TagKey<EntityType<?>> PREVIEW_NATIVE_SCALE = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("spiral_abyss", "preview_native_scale"));
    private final Map<ResourceLocation, LivingEntity> entities = new HashMap<>();
    private final Map<ResourceLocation, TextureTarget> portraits = new HashMap<>();
    private final Map<ResourceLocation, PreviewAnimation> animations = new HashMap<>();
    private final Map<Integer, Crop> crops = new HashMap<>();
    record Bounds(int x, int y, int width, int height) {}
    private final Set<ResourceLocation> unavailable = new HashSet<>();
    private final long cycleSeed;
    private final Random poses = new Random(0);

    BannerBossRenderer(long cycleSeed) { this.cycleSeed = cycleSeed; }

    /** Initialize render-only state without spawning or ticking optional mobs. */
    static void initializeModel(LivingEntity entity) {
        // Opt-in native AgeTicks contract: both dragon editions otherwise render hatchlings.
        if (entity.getType().is(PREVIEW_ADULT_AGE_TICKS))
            MobSpawnData.apply(entity, MobSpawnData.parse("{AgeTicks:2400000,AgingDisabled:1b}"));
        // Sea Serpent's regular renderer multiplies by a constructor-default scale of zero.
        // Its native float Scale contract is shared with CE; no AI tick/finalizeSpawn is needed.
        if (entity.getType().is(PREVIEW_NATIVE_SCALE))
            MobSpawnData.apply(entity, MobSpawnData.parse("{Scale:3.0f,Ancient:1b}"));
        if (!entity.getType().is(PREVIEW_UPRIGHT_NORMALS)) return;
        // Tagged models expose these public Vec3 fields, normally initialized
        // by ticking. Give unspawned previews a valid upright orientation.
        try {
            var up = new Vec3(0, 1, 0);
            entity.getClass().getField("normal").set(entity, up);
            entity.getClass().getField("lastNormal").set(entity, up);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot initialize tagged preview surface normals", exception);
        }
    }

    Component name(ResourceLocation id) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(id);
        return type == null ? Component.literal(id.toString()) : type.getDescription();
    }

    void clear() {
        // TextureTarget.destroyBuffers unbinds the current framebuffer as a side effect.
        int framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        try {
            portraits.values().forEach(TextureTarget::destroyBuffers);
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        }
        portraits.clear();
        crops.clear();
        entities.clear();
        animations.clear();
        unavailable.clear();
    }

    void render(GuiGraphics graphics, ResourceLocation id, int x, int y, int width, int height) {
        render(graphics, id, PreviewAnimation.DEFAULT, x, y, width, height);
    }

    void render(GuiGraphics graphics, ResourceLocation id, PreviewAnimation animation, int x, int y, int width, int height) {
        if (width < 1 || height < 1) return;
        if (!animation.equals(animations.put(id, animation))) {
            TextureTarget oldPortrait = portraits.remove(id);
            if (oldPortrait != null) destroyPortrait(oldPortrait);
            unavailable.remove(id);
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || unavailable.contains(id)) {
            fallback(graphics, id, x, y, width, height);
            return;
        }
        LivingEntity entity = entities.get(id);
        // A disconnect/world replacement must not retain an instance belonging to the previous level.
        if (entity == null || entity.level() != client.level) {
            TextureTarget oldPortrait = portraits.remove(id);
            if (oldPortrait != null) destroyPortrait(oldPortrait);
            try {
                var type = BuiltInRegistries.ENTITY_TYPE.get(id);
                var created = type == null ? null : type.create(client.level);
                if (!(created instanceof LivingEntity living)) {
                    unavailable.add(id);
                    fallback(graphics, id, x, y, width, height);
                    return;
                }
                long seed = cycleSeed;
                for (char c : id.toString().toCharArray()) seed = (seed ^ c) * 0x100000001b3L;
                poses.setSeed(seed);
                living.getRandom().setSeed(seed);
                living.setUUID(new UUID(seed, ~seed));
                initializeModel(living);
                samplePose(living);
                entity = living;
                entities.put(id, living);
            } catch (RuntimeException | LinkageError exception) {
                unavailable.add(id);
                LogUtils.getLogger().warn("Cannot create Abyss banner model {}", id, exception);
                fallback(graphics, id, x, y, width, height);
                return;
            }
        }

        TextureTarget portrait = portraits.get(id);
        try {
            if (portrait == null) {
                graphics.flush();
                for (int attempt = 0; attempt < 10; attempt++) {
                    portrait = capture(graphics, entity, animation, (float) Math.pow(.55, attempt));
                    Crop crop = crops.get(portrait.getColorTextureId());
                    if (attempt == 9 || (crop.x() > 0 && crop.y() > 0
                            && crop.x() + crop.width() < portrait.width
                            && crop.y() + crop.height() < portrait.height)) break;
                    destroyPortrait(portrait);
                }
                portraits.put(id, portrait);
            }
            BannerPortrait.draw(graphics, portrait, crops.get(portrait.getColorTextureId()), x, y, width, height);
        } catch (RuntimeException | LinkageError exception) {
            unavailable.add(id);
            LogUtils.getLogger().warn("Cannot render Abyss banner model {}", id, exception);
        }
        if (unavailable.contains(id)) fallback(graphics, id, x, y, width, height);
    }

    /** Capturing once freezes even renderers that read their own clock instead of entity age. */
    private TextureTarget capture(GuiGraphics graphics, LivingEntity entity, PreviewAnimation animation, float zoom) {
        Minecraft client = Minecraft.getInstance();
        var dispatcher = client.getEntityRenderDispatcher();
        Quaternionf previousCamera = new Quaternionf(dispatcher.cameraOrientation());
        RenderState previous = new RenderState();
        TextureTarget target = null;
        boolean success = false;
        try {
            int pixelWidth = 512, pixelHeight = 512;
            target = new TextureTarget(pixelWidth, pixelHeight, true, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
            target.setClearColor(0, 0, 0, 0);
            RenderSystem.disableScissor();
            RenderSystem.depthMask(true);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, pixelWidth, pixelHeight, 0,
                    1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.getModelViewStack().pushMatrix();
            try {
                RenderSystem.getModelViewStack().identity();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                Lighting.setupForEntityInInventory();
                dispatcher.overrideCameraOrientation(new Quaternionf());

                boolean dragon = entity instanceof EnderDragon;
                float modelWidth = dragon ? 17F : Math.max(0.9F, entity.getBbWidth() * 1.8F);
                float modelHeight = dragon ? 9F : Math.max(1F, entity.getBbHeight() * 1.22F);
                float scale = zoom * .65F * Math.min((pixelWidth - 24F) / modelWidth, (pixelHeight - 24F) / modelHeight);
                PoseStack modelPose = new PoseStack();
                modelPose.translate(pixelWidth / 2F, pixelHeight * (dragon || OptionalGeoPreview.supports(entity, animation) ? 0.54F : 0.82F), -11000);
                modelPose.scale(scale, scale, -scale);
                modelPose.mulPose(new Quaternionf().rotateZ((float) Math.PI)
                        .rotateY((float) Math.toRadians(dragon ? 150 : -16)));
                // Direct registered renderer avoids world shadows and changes to shadow settings.
                RenderSystem.runAsFancy(() -> {
                    if (!OptionalGeoPreview.render(entity, entity.getUUID().getMostSignificantBits(), animation,
                            modelPose, graphics.bufferSource(), 15728880)) {
                        dispatcher.getRenderer(entity).render(entity, 0F, 1F,
                                modelPose, graphics.bufferSource(), 15728880);
                    }
                });
                graphics.flush();
                crops.put(target.getColorTextureId(), BannerPortrait.finish(target));
                success = true;
                return target;
            } finally {
                try {
                    if (!success) graphics.flush();
                } finally {
                    RenderSystem.getModelViewStack().popMatrix();
                    RenderSystem.applyModelViewMatrix();
                }
            }
        } finally {
            try {
                if (!success && target != null) target.destroyBuffers();
            } finally {
                dispatcher.overrideCameraOrientation(previousCamera);
                Lighting.setupFor3DItems();
                previous.restore();
            }
        }
    }

    private void destroyPortrait(TextureTarget portrait) {
        crops.remove(portrait.getColorTextureId());
        int framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        try {
            portrait.destroyBuffers();
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        }
    }

    Bounds contentBounds(ResourceLocation id, int x, int y, int width, int height) {
        var target = portraits.get(id);
        Crop crop = target == null ? null : crops.get(target.getColorTextureId());
        if (crop == null) return new Bounds(x, y, width, height);
        float scale = Math.min((float) width / crop.width(), (float) height / crop.height());
        int fittedWidth = Math.round(crop.width() * scale), fittedHeight = Math.round(crop.height() * scale);
        return new Bounds(x + (width - fittedWidth) / 2, y + (height - fittedHeight) / 2, fittedWidth, fittedHeight);
    }

    private void samplePose(LivingEntity entity) {
        entity.tickCount = 50 + poses.nextInt(100);
        entity.setYRot(180);
        entity.yRotO = entity.yBodyRot = entity.yBodyRotO = 180;
        entity.yHeadRot = entity.yHeadRotO = 168 + poses.nextFloat() * 24;
        entity.setXRot(-8 + poses.nextFloat() * 12);
        entity.xRotO = entity.getXRot();
        entity.attackAnim = entity.oAttackAnim = 0.22F + poses.nextFloat() * 0.52F;
        // Only update the animation value holder. This cannot move, spawn, target or tick anything.
        for (int step = 0, count = 5 + poses.nextInt(16); step < count; step++) {
            entity.walkAnimation.update(0.65F + poses.nextFloat() * 0.3F, 1F);
        }
        if (entity instanceof Mob mob) mob.setAggressive(true);
        if (entity instanceof EnderDragon dragon) {
            dragon.flapTime = dragon.oFlapTime = 0.08F + poses.nextFloat() * 0.42F;
            dragon.posPointer = 63;
            for (int i = 0; i < dragon.positions.length; i++) {
                dragon.positions[i][0] = 180 + Math.sin(i * 0.16) * 7;
                dragon.positions[i][1] = Math.sin(i * 0.12) * 0.35;
                dragon.positions[i][2] = 0;
            }
        } else if (entity instanceof Warden warden) {
            if (poses.nextBoolean()) warden.roarAnimationState.start(entity.tickCount - 16 - poses.nextInt(20));
            else warden.attackAnimationState.start(entity.tickCount - 3 - poses.nextInt(5));
        } else {
            sampleExposedAnimation(entity);
        }
        sampleLibraryAnimation(entity);
    }

    /** Optional animation APIs: set a local clip/time only, never dispatch network/AI handlers. */
    private void sampleLibraryAnimation(LivingEntity entity) {
        for (String api : List.of("com.github.alexthe666.citadel.animation.IAnimatedEntity",
                "com.github.L_Ender.lionfishapi.server.animation.IAnimatedEntity")) {
            try {
                Class<?> animated = Class.forName(api, false, entity.getClass().getClassLoader());
                if (!animated.isInstance(entity)) continue;
                Object[] animations = (Object[]) animated.getMethod("getAnimations").invoke(entity);
                Object none = animated.getField("NO_ANIMATION").get(null);
                List<Object> choices = new ArrayList<>();
                for (Object animation : animations) {
                    if (animation != null && animation != none
                            && (int) animation.getClass().getMethod("getDuration").invoke(animation) > 4) {
                        choices.add(animation);
                    }
                }
                if (choices.isEmpty()) return;
                Object animation = choices.get(poses.nextInt(choices.size()));
                int duration = (int) animation.getClass().getMethod("getDuration").invoke(animation);
                animated.getMethod("setAnimation", animation.getClass()).invoke(entity, animation);
                animated.getMethod("setAnimationTick", int.class).invoke(entity,
                        Math.max(1, Math.round(duration * (0.2F + poses.nextFloat() * 0.5F))));
                return;
            } catch (ClassNotFoundException ignored) {
                // Optional mod is absent.
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                LogUtils.getLogger().debug("Cannot sample optional Abyss banner animation {}", api, exception);
            }
        }
    }

    /** Supports mods using vanilla animation states without a dependency or invoking attack logic. */
    private void sampleExposedAnimation(LivingEntity entity) {
        List<AnimationState> candidates = new ArrayList<>();
        Field[] fields = entity.getClass().getFields();
        Arrays.sort(fields, Comparator.comparing(field -> field.getDeclaringClass().getName() + ":" + field.getName()));
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType() != AnimationState.class) continue;
            String name = field.getName().toLowerCase(Locale.ROOT);
            if (!(name.contains("attack") || name.contains("roar") || name.contains("swing")
                    || name.contains("walk") || name.contains("idle"))) continue;
            try {
                if (field.get(entity) instanceof AnimationState animation) candidates.add(animation);
            } catch (IllegalAccessException ignored) {
                // Encapsulated animation engines keep the sampled vanilla pose instead.
            }
        }
        if (!candidates.isEmpty()) {
            candidates.get(poses.nextInt(candidates.size())).start(entity.tickCount - 4 - poses.nextInt(12));
        }
    }

    private void fallback(GuiGraphics graphics, ResourceLocation id, int x, int y, int width, int height) {
        var font = Minecraft.getInstance().font;
        var lines = font.split(name(id), Math.max(1, width - 8));
        int visibleLines = Math.min(lines.size(), Math.max(1, height / font.lineHeight));
        int top = y + (height - visibleLines * font.lineHeight) / 2;
        for (int i = 0; i < visibleLines; i++) {
            graphics.drawCenteredString(font, lines.get(i), x + width / 2,
                    top + i * font.lineHeight, 0xFFE9DCC4);
        }
    }
}
