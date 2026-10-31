package com.pla.spiral_abyss.client.screen;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.BossOptions.PreviewAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.loading.math.MolangQueries;
import software.bernie.geckolib.loading.math.value.Variable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.constant.DataTickets;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.io.IOException;
import java.util.function.DoubleSupplier;
import software.bernie.geckolib.constant.dataticket.DataTicket;

/** Loaded only behind the optional GeckoLib gate. Never ticks entities or executes clip events. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class GeckoPreviewSampler {
    private record Failure(Class<?> type, PreviewAnimation animation) {}
    private static final Set<Failure> failed = new HashSet<>();

    static boolean supports(LivingEntity entity) {
        return entity instanceof GeoAnimatable
                && Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity) instanceof GeoEntityRenderer;
    }

    static boolean render(LivingEntity entity, long seed, PoseStack pose, MultiBufferSource buffers, int light) {
        return render(entity, seed, PreviewAnimation.DEFAULT, pose, buffers, light);
    }

    static boolean render(LivingEntity entity, long seed, PreviewAnimation preview, PoseStack pose, MultiBufferSource buffers, int light) {
        Failure failure = new Failure(entity.getClass(), preview);
        if (failed.contains(failure)) return false;
        GeoEntityRenderer renderer = (GeoEntityRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        GeoAnimatable animated = (GeoAnimatable) entity;
        List<SavedField> rendererState = new ArrayList<>();
        List<SavedField> boneState = new ArrayList<>();
        List<SavedField> queryState = new ArrayList<>();
        Map<String, Variable> queries = null;
        Map<Variable, DoubleSupplier> queryValues = new IdentityHashMap<>();
        Field actorField = null;
        Object savedActor = null;
        AnimationProcessor processor = null;
        boolean reload = false;
        pose.pushPose();
        try {
            Field variables = MolangQueries.class.getDeclaredField("VARIABLES");
            variables.setAccessible(true);
            queries = (Map<String, Variable>) variables.get(null);
            queries.values().forEach(variable -> queryValues.put(variable, variable.value().get()));
            actorField = MolangQueries.class.getDeclaredField("ACTOR");
            actorField.setAccessible(true);
            savedActor = actorField.get(null);
            // Gecko renderers use this field for model variants. Scope it to the preview,
            // including when a transformed world entity was the last rendered instance.
            for (String name : List.of("animatable", "entityRenderTranslations", "modelRenderTranslations")) {
                Field field = GeoEntityRenderer.class.getDeclaredField(name);
                field.setAccessible(true);
                rendererState.add(new SavedField(renderer, field, copy(field.get(renderer))));
                if (name.equals("animatable")) field.set(renderer, entity);
            }
            GeoModel model = renderer.getGeoModel();
            var baked = model.getBakedModel(model.getModelResource(animated, renderer));
            processor = model.getAnimationProcessor();
            reload = processor.reloadAnimations;
            for (Object object : processor.getRegisteredBones()) {
                GeoBone bone = (GeoBone) object;
                // Baked bones are globally shared. Save flags and matrices as well as transforms.
                for (Field field : GeoBone.class.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    if (Modifier.isFinal(field.getModifiers()) && field.getType() != Matrix4f.class
                            && field.getType() != Matrix3f.class) continue;
                    if (!(field.getType().isPrimitive() || field.getType() == Boolean.class
                            || field.getType() == Matrix4f.class || field.getType() == Matrix3f.class)) continue;
                    field.setAccessible(true);
                    boneState.add(new SavedField(bone, field, copy(field.get(bone))));
                }
                reset(bone);
            }
            List<String> choices = preview.configured() ? List.of(preview.name()) : clips(model, animated);
            if (choices.isEmpty()) return false;
            Random random = new Random(seed);
            String clip = choices.get(random.nextInt(choices.size()));
            Animation animation = model.getAnimation(animated, clip);
            if (animation == null) throw new IllegalArgumentException("Unknown preview animation: " + clip);
            double sample = preview.configured()
                    ? Math.min(preview.timeSeconds() * 20, Math.max(0, animation.length() - .001))
                    : Math.max(1, Math.min(160, animation.length()) * (.2 + random.nextDouble() * .5));
            RawAnimation raw = RawAnimation.begin().thenLoop(clip);
            // A new manager is independent of both the entity cache and world controllers.
            AnimatableManager manager = new AnimatableManager(animated);
            manager.getAnimationControllers().clear();
            AnimationController controller = new AnimationController(animated, "abyss_preview", 0, state -> {
                state.setAnimation(raw);
                return PlayState.CONTINUE;
            });
            controller.setSoundKeyframeHandler(event -> {});
            controller.setParticleKeyframeHandler(event -> {});
            controller.setCustomInstructionKeyframeHandler(event -> {});
            manager.addController(controller);
            AnimationState state = new AnimationState(animated, 0, 0, 0, false);
            state.setData(DataTickets.ENTITY, entity);
            state.setData(DataTickets.ENTITY_MODEL_DATA, new EntityModelData(false, false, 0, 0));
            state.setData(DataTickets.TICK, sample);
            model.addAdditionalStateData(animated, renderer.getInstanceId(entity),
                    (key, value) -> state.setData((DataTicket) key, value));
            processor.reloadAnimations = false;
            // Prime clip/transition at zero before seeking into the chosen animation.
            for (double time : new double[] {0, 0, sample}) {
                state.animationTick = time;
                processor.preAnimationSetup(state, time);
                processor.tickAnimation(animated, model, manager, time, state, false);
            }
            model.setCustomAnimations(animated, renderer.getInstanceId(entity), state);
            var texture = renderer.getTextureLocation(entity);
            var type = renderer.getRenderType(animated, texture, buffers, 0);
            if (type == null) return false;
            // Explicit frames render just the sampled mesh. A mod's renderer may require
            // live combat lighting/effect helpers that are absent on an unspawned preview.
            GeoRenderer mesh = preview.configured() ? new PreviewMesh(model, animated) : renderer;
            mesh.reRender(baked, pose, buffers, animated, type, buffers.getBuffer(type), 0,
                    light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            LogUtils.getLogger().debug("Abyss preview {} samples {} at {} ticks", entity.getType(), clip, sample);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            failed.add(failure);
            LogUtils.getLogger().warn("Optional Abyss animation preview unavailable for {}", entity.getType(), exception);
            return false;
        } finally {
            for (SavedField field : boneState) field.restore();
            for (SavedField field : rendererState) field.restore();
            for (SavedField field : queryState) field.restore();
            queryValues.forEach(Variable::set);
            if (queries != null) for (Variable variable : queries.values())
                if (!queryValues.containsKey(variable)) variable.set(() -> 0);
            if (actorField != null) {
                try { actorField.set(null, savedActor); }
                catch (IllegalAccessException exception) { throw new IllegalStateException(exception); }
            }
            if (processor != null) processor.reloadAnimations = reload;
            pose.popPose();
        }
    }

    private record PreviewMesh(GeoModel model, GeoAnimatable entity) implements GeoRenderer<GeoAnimatable> {
        @Override public GeoModel getGeoModel() { return model; }
        @Override public GeoAnimatable getAnimatable() { return entity; }
        @Override public void fireCompileRenderLayersEvent() {}
        @Override public boolean firePreRenderEvent(PoseStack pose, BakedGeoModel model,
                                                    MultiBufferSource buffers, float tick, int light) { return true; }
        @Override public void firePostRenderEvent(PoseStack pose, BakedGeoModel model,
                                                 MultiBufferSource buffers, float tick, int light) {}
        @Override public void updateAnimatedTextureFrame(GeoAnimatable entity) {}
    }

    private static List<String> clips(GeoModel model, GeoAnimatable entity) {
        ResourceLocation path = model.getAnimationResource(entity);
        var resource = Minecraft.getInstance().getResourceManager().getResource(path);
        if (resource.isEmpty()) return List.of();
        try (var reader = resource.get().openAsReader()) {
            var animations = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
            if (animations == null) return List.of();
            List<String> names = new ArrayList<>();
            for (String name : animations.keySet()) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("death") || lower.contains("die") || lower.contains("spawn")
                        || lower.contains("intro") || lower.contains("cinematic") || lower.contains("transform")
                        || lower.contains("transition") || lower.contains("entrance") || lower.contains("layer")
                        || lower.contains("overlay") || lower.contains("hurt") || lower.contains("stun")
                        || lower.contains("knocked") || lower.contains("dive") || lower.contains("sand_move")
                        || lower.contains("_old") || lower.contains("hit") || lower.contains("sleep") || lower.contains("frozen")) continue;
                Animation animation = model.getAnimation(entity, name);
                if (animation != null && animation.length() > 4 && Double.isFinite(animation.length())) names.add(name);
            }
            Collections.sort(names);
            return names;
        } catch (IOException exception) {
            return List.of();
        }
    }

    private static void reset(GeoBone bone) {
        BoneSnapshot initial = bone.getInitialSnapshot();
        bone.setRotX(initial.getRotX()); bone.setRotY(initial.getRotY()); bone.setRotZ(initial.getRotZ());
        bone.setPosX(initial.getOffsetX()); bone.setPosY(initial.getOffsetY()); bone.setPosZ(initial.getOffsetZ());
        bone.setScaleX(initial.getScaleX()); bone.setScaleY(initial.getScaleY()); bone.setScaleZ(initial.getScaleZ());
        bone.setHidden(Boolean.TRUE.equals(bone.shouldNeverRender())); bone.resetStateChanges();
    }

    private static Object copy(Object value) {
        if (value instanceof Matrix4f matrix) return new Matrix4f(matrix);
        if (value instanceof Matrix3f matrix) return new Matrix3f(matrix);
        return value;
    }

    private record SavedField(Object owner, Field field, Object value) {
        void restore() {
            try {
                if (value instanceof Matrix4f matrix) ((Matrix4f) field.get(owner)).set(matrix);
                else if (value instanceof Matrix3f matrix) ((Matrix3f) field.get(owner)).set(matrix);
                else field.set(owner, value);
            }
            catch (IllegalAccessException exception) { throw new IllegalStateException(exception); }
        }
    }
}
