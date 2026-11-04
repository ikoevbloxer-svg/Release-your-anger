package com.example.crackwhip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/** Рисует цепь между привязанным существом и держателем. */
@EventBusSubscriber(modid = CrackWhip.MODID, value = Dist.CLIENT)
public class ChainRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CrackWhip.MODID, "textures/entity/chain_link.png");
    private static final int SEGMENTS = 22;
    private static final double HALF_WIDTH = 0.06;
    private static final double SAG = 0.35;

    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Integer holderId = ClientChains.get(entity.getId());
        if (holderId == null) return;
        Entity holder = entity.level().getEntity(holderId);
        if (holder == null) return;

        float pt = event.getPartialTick();
        // PoseStack здесь уже сдвинут в позицию ног рисуемого существа
        Vec3 start = new Vec3(0, entity.getBbHeight() * 0.6, 0);
        Vec3 end = holder.getPosition(pt).add(0, holder.getBbHeight() * 0.6, 0)
                .subtract(entity.getPosition(pt));

        MultiBufferSource buffers = event.getMultiBufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = event.getPoseStack().last();
        int light = event.getPackedLight();

        Vec3 prev = point(start, end, 0.0);
        for (int i = 1; i <= SEGMENTS; i++) {
            Vec3 cur = point(start, end, i / (double) SEGMENTS);
            segment(vc, pose, prev, cur, light);
            prev = cur;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientChains.clear();
    }

    private static Vec3 point(Vec3 a, Vec3 b, double t) {
        double sag = SAG * 4.0 * t * (1.0 - t);
        return new Vec3(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t - sag,
                a.z + (b.z - a.z) * t);
    }

    /** Два скрещённых прямоугольника — цепь видна с любой стороны. */
    private static void segment(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, int light) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1.0E-8) return;
        dir = dir.normalize();
        Vec3 side1 = dir.cross(new Vec3(0, 1, 0));
        if (side1.lengthSqr() < 1.0E-6) side1 = new Vec3(1, 0, 0);
        side1 = side1.normalize().scale(HALF_WIDTH);
        Vec3 side2 = dir.cross(side1).normalize().scale(HALF_WIDTH);
        quad(vc, pose, a, b, side1, light);
        quad(vc, pose, a, b, side2, light);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 s, int light) {
        vertex(vc, pose, a.add(s), 0f, 0f, light);
        vertex(vc, pose, a.subtract(s), 1f, 0f, light);
        vertex(vc, pose, b.subtract(s), 1f, 1f, light);
        vertex(vc, pose, b.add(s), 0f, 1f, light);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, int light) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
    }
}
