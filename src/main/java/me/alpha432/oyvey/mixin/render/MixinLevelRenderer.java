package me.alpha432.oyvey.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.impl.render.RenderBlockOutlineEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.alpha432.oyvey.util.traits.Util.EVENT_BUS;
import static me.alpha432.oyvey.util.traits.Util.mc;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    public void renderHitOutline(CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderBlockOutlineEvent())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void render(DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera,
                        GameRenderer gameRenderer, LightTexture lightTexture,
                        Matrix4f positionMatrix, Matrix4f projectionMatrix,
                        CallbackInfo ci, @Local ProfilerFiller profiler) {

        PoseStack stack = new PoseStack();
        stack.pushPose();
        stack.mulPose(Axis.XP.rotationDegrees(mc.gameRenderer.getMainCamera().getXRot()));
        stack.mulPose(Axis.YP.rotationDegrees(mc.gameRenderer.getMainCamera().getYRot() + 180f));

        profiler.push("oyvey-render-3d");

        Render3DEvent event = new Render3DEvent(stack, tickCounter.getGameTimeDeltaPartialTick(true));
        EVENT_BUS.post(event);
        stack.popPose();
        profiler.pop();
    }
}
