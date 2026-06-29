package me.alpha432.oyvey.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.awt.*;

public class RenderUtil implements Util {

    /** Draws a 1px line between two screen-space points using the GuiGraphics buffer. */
    public static void draw2DLine(GuiGraphics context, float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        float nx = -dy / len * 0.5f;
        float ny =  dx / len * 0.5f;
        org.joml.Matrix4f pose = context.pose().last().pose();
        var buf = context.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());
        buf.addVertex(pose, x1 + nx, y1 + ny, 0).setColor(color);
        buf.addVertex(pose, x1 - nx, y1 - ny, 0).setColor(color);
        buf.addVertex(pose, x2 - nx, y2 - ny, 0).setColor(color);
        buf.addVertex(pose, x2 + nx, y2 + ny, 0).setColor(color);
        context.bufferSource().endBatch(net.minecraft.client.renderer.RenderType.gui());
    }

    public static void rect(GuiGraphics context, float x1, float y1, float x2, float y2, int color) {
        context.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), color);
    }

    public static void rect(GuiGraphics context, float x1, float y1, float x2, float y2, int color, float width) {
        int w = Math.max(1, Math.round(width));
        context.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y1) + w, color);
        context.fill(Math.round(x2) - w, Math.round(y1), Math.round(x2), Math.round(y2), color);
        context.fill(Math.round(x1), Math.round(y2) - w, Math.round(x2), Math.round(y2), color);
        context.fill(Math.round(x1), Math.round(y1), Math.round(x1) + w, Math.round(y2), color);
    }

    public static void horizontalGradient(GuiGraphics context, float x1, float y1, float x2, float y2, Color left, Color right) {
        gradient(context, Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2),
                left.hashCode(), left.hashCode(), right.hashCode(), right.hashCode());
    }

    public static void verticalGradient(GuiGraphics context, float x1, float y1, float x2, float y2, Color top, Color bottom) {
        gradient(context, Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2),
                top.hashCode(), bottom.hashCode(), bottom.hashCode(), top.hashCode());
    }

    public static void gradient(GuiGraphics graphics,
                                int x1, int y1, int x2, int y2,
                                int topLeft, int bottomLeft, int bottomRight, int topRight) {
        Matrix4f pose = graphics.pose().last().pose();
        var consumer = graphics.bufferSource().getBuffer(RenderType.gui());
        consumer.addVertex(pose, x1, y1, 0).setColor(topLeft);
        consumer.addVertex(pose, x1, y2, 0).setColor(bottomLeft);
        consumer.addVertex(pose, x2, y2, 0).setColor(bottomRight);
        consumer.addVertex(pose, x2, y1, 0).setColor(topRight);
    }

    public static void rect(PoseStack stack, float x1, float y1, float x2, float y2, int color) {
        rectFilled(stack, x1, y1, x2, y2, color);
    }

    public static void rect(PoseStack stack, float x1, float y1, float x2, float y2, int color, float width) {
        drawHorizontalLine(stack, x1, x2, y1, color, width);
        drawVerticalLine(stack, x2, y1, y2, color, width);
        drawHorizontalLine(stack, x1, x2, y2, color, width);
        drawVerticalLine(stack, x1, y1, y2, color, width);
    }

    protected static void drawHorizontalLine(PoseStack matrices, float x1, float x2, float y, int color) {
        if (x2 < x1) { float i = x1; x1 = x2; x2 = i; }
        rectFilled(matrices, x1, y, x2 + 1, y + 1, color);
    }

    protected static void drawVerticalLine(PoseStack matrices, float x, float y1, float y2, int color) {
        if (y2 < y1) { float i = y1; y1 = y2; y2 = i; }
        rectFilled(matrices, x, y1 + 1, x + 1, y2, color);
    }

    protected static void drawHorizontalLine(PoseStack matrices, float x1, float x2, float y, int color, float width) {
        if (x2 < x1) { float i = x1; x1 = x2; x2 = i; }
        rectFilled(matrices, x1, y, x2 + width, y + width, color);
    }

    protected static void drawVerticalLine(PoseStack matrices, float x, float y1, float y2, int color, float width) {
        if (y2 < y1) { float i = y1; y1 = y2; y2 = i; }
        rectFilled(matrices, x, y1 + width, x + width, y2, color);
    }

    public static void rectFilled(PoseStack matrix, float x1, float y1, float x2, float y2, int color) {
        float i;
        if (x1 < x2) { i = x1; x1 = x2; x2 = i; }
        if (y1 < y2) { i = y1; y1 = y2; y2 = i; }

        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float j = (float) (color & 255) / 255.0F;

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.addVertex(matrix.last().pose(), x1, y2, 0.0F).setColor(g, h, j, f);
        buf.addVertex(matrix.last().pose(), x2, y2, 0.0F).setColor(g, h, j, f);
        buf.addVertex(matrix.last().pose(), x2, y1, 0.0F).setColor(g, h, j, f);
        buf.addVertex(matrix.last().pose(), x1, y1, 0.0F).setColor(g, h, j, f);

        RenderType layer = Layers.GLOBAL_QUADS;
        layer.setupRenderState();
        BufferUploader.drawWithShader(buf.buildOrThrow());
        layer.clearRenderState();
    }

    public static void horizontalGradient(PoseStack matrix, float x1, float y1, float x2, float y2, Color left, Color right) {
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.addVertex(matrix.last().pose(), x1, y1, 0.0F).setColor(left.getRed() / 255.0F, left.getGreen() / 255.0F, left.getBlue() / 255.0F, left.getAlpha() / 255.0F);
        buf.addVertex(matrix.last().pose(), x1, y2, 0.0F).setColor(left.getRed() / 255.0F, left.getGreen() / 255.0F, left.getBlue() / 255.0F, left.getAlpha() / 255.0F);
        buf.addVertex(matrix.last().pose(), x2, y2, 0.0F).setColor(right.getRed() / 255.0F, right.getGreen() / 255.0F, right.getBlue() / 255.0F, right.getAlpha() / 255.0F);
        buf.addVertex(matrix.last().pose(), x2, y1, 0.0F).setColor(right.getRed() / 255.0F, right.getGreen() / 255.0F, right.getBlue() / 255.0F, right.getAlpha() / 255.0F);

        RenderType layer = Layers.GLOBAL_QUADS;
        layer.setupRenderState();
        BufferUploader.drawWithShader(buf.buildOrThrow());
        layer.clearRenderState();
    }

    public static void verticalGradient(PoseStack matrix, float x1, float y1, float x2, float y2, Color top, Color bottom) {
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.addVertex(matrix.last().pose(), x1, y1, 0.0F).setColor(top.getRed() / 255.0F, top.getGreen() / 255.0F, top.getBlue() / 255.0F, top.getAlpha() / 255.0F);
        buf.addVertex(matrix.last().pose(), x1, y2, 0.0F).setColor(bottom.getRed() / 255.0F, bottom.getGreen() / 255.0F, bottom.getBlue() / 255.0F, bottom.getAlpha() / 255.0F);
        buf.addVertex(matrix.last().pose(), x2, y2, 0.0F).setColor(bottom.getRed() / 255.0F, bottom.getGreen() / 255.0F, bottom.getBlue() / 255.0F, bottom.getAlpha() / 255.0F);
        buf.addVertex(matrix.last().pose(), x2, y1, 0.0F).setColor(top.getRed() / 255.0F, top.getGreen() / 255.0F, top.getBlue() / 255.0F, top.getAlpha() / 255.0F);

        RenderType layer = Layers.GLOBAL_QUADS;
        layer.setupRenderState();
        BufferUploader.drawWithShader(buf.buildOrThrow());
        layer.clearRenderState();
    }

    // 3D
    public static void drawBoxFilled(PoseStack stack, AABB box, Color c) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getPosition().x());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getPosition().y());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getPosition().x());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getPosition().y());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getPosition().z());

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.addVertex(stack.last().pose(), minX, minY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, minY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, minY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, minY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, maxY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, maxY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, maxY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, maxY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, minY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, maxY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, maxY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, minY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, minY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, maxY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, maxY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, minY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, minY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, minY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), maxX, maxY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, maxY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, minY, minZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, minY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, maxY, maxZ).setColor(c.getRGB());
        buf.addVertex(stack.last().pose(), minX, maxY, minZ).setColor(c.getRGB());

        RenderType layer = Layers.GLOBAL_QUADS;
        layer.setupRenderState();
        BufferUploader.drawWithShader(buf.buildOrThrow());
        layer.clearRenderState();
    }

    public static void drawBoxFilled(PoseStack stack, Vec3 vec, Color c) {
        drawBoxFilled(stack, AABB.unitCubeFromLowerCorner(vec), c);
    }

    public static void drawBoxFilled(PoseStack stack, BlockPos bp, Color c) {
        drawBoxFilled(stack, new AABB(bp), c);
    }

    public static void drawBox(PoseStack stack, AABB box, Color c, float lineWidth) {
        drawBox(stack, Shapes.create(box), c, lineWidth);
    }

    public static void drawBox(PoseStack stack, VoxelShape shape, Color c, float lineWidth) {
        Vec3 camera = mc.getEntityRenderDispatcher().camera.getPosition();

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        PoseStack.Pose pose = stack.last();
        int color = c.getRGB();

        shape.forAllEdges((x1, y1, z1, x2, y2, z2) ->
                addLine(buf, pose, color,
                        x1 - camera.x, y1 - camera.y, z1 - camera.z,
                        x2 - camera.x, y2 - camera.y, z2 - camera.z)
        );

        RenderSystem.lineWidth(lineWidth);
        RenderType layer = Layers.GLOBAL_LINES;
        layer.setupRenderState();
        BufferUploader.drawWithShader(buf.buildOrThrow());
        layer.clearRenderState();
        RenderSystem.lineWidth(1.0f);
    }

    private static void addLine(BufferBuilder buf, PoseStack.Pose pose, int color,
                                double x1, double y1, double z1, double x2, double y2, double z2) {
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        buf.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(color).setNormal(pose, nx, ny, nz);
        buf.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(color).setNormal(pose, nx, ny, nz);
    }

    public static void drawBox(PoseStack stack, Vec3 vec, Color c, float lineWidth) {
        drawBox(stack, AABB.unitCubeFromLowerCorner(vec), c, lineWidth);
    }

    public static void drawBox(PoseStack stack, BlockPos bp, Color c, float lineWidth) {
        drawBox(stack, new AABB(bp), c, lineWidth);
    }

    public static PoseStack matrixFrom(Vec3 pos) {
        PoseStack matrices = new PoseStack();
        Camera camera = mc.gameRenderer.getMainCamera();
        matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
        matrices.translate(pos.x() - camera.getPosition().x, pos.y() - camera.getPosition().y, pos.z() - camera.getPosition().z);
        return matrices;
    }
}
