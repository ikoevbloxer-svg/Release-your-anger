package com.example.crackwhip;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Серверная логика цепного поводка.
 * Держатель хранится в NBT сущности (переживает перезапуск сервера).
 * Цепь НИКОГДА не рвётся: если далеко — тянет, если очень далеко — телепортирует к держателю.
 * Снять можно только ПКМ цепью по привязанному.
 */
@EventBusSubscriber(modid = CrackWhip.MODID)
public class Chains {
    public static final String KEY = "crackwhip_holder";
    /** Свободная длина цепи в блоках. */
    public static final double SLACK = 5.0;
    /** Дальше этого расстояния привязанного телепортирует к держателю. */
    public static final double TELEPORT_DISTANCE = 24.0;

    @Nullable
    public static UUID getHolderId(Entity e) {
        CompoundTag tag = e.getPersistentData();
        return tag.hasUUID(KEY) ? tag.getUUID(KEY) : null;
    }

    public static void attach(LivingEntity target, Player holder) {
        target.getPersistentData().putUUID(KEY, holder.getUUID());
        if (target instanceof Mob mob) {
            mob.setPersistenceRequired(); // чтобы моб не исчез (despawn)
        }
        sync(target, holder);
    }

    public static void release(LivingEntity target) {
        target.getPersistentData().remove(KEY);
        sync(target, null);
    }

    private static void sync(LivingEntity target, @Nullable Entity holder) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                new ChainSyncPayload(target.getId(), holder == null ? -1 : holder.getId()));
    }

    // ПКМ цепью по сущности: привязать / отвязать (перехватываем раньше жителей, волков, лошадей)
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getItemStack().getItem() instanceof ChainLeashItem)) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        Level level = event.getLevel();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if (level.isClientSide()) return;

        Player player = event.getEntity();
        if (target == player) return;

        if (player.getUUID().equals(getHolderId(target))) {
            release(target);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            attach(target, player); // если был привязан к другому — цепь перехватывается
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            tick(living);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        tick(event.getEntity());
    }

    private static void tick(LivingEntity e) {
        if (!(e.level() instanceof ServerLevel level)) return;
        UUID holderId = getHolderId(e);
        if (holderId == null) return;

        Entity holder = level.getEntity(holderId);
        if (e.tickCount % 10 == 0) {
            sync(e, holder); // раз в полсекунды обновляем клиентов (вход в зону видимости, возвращение держателя)
        }
        if (holder == null || !holder.isAlive() || !e.isAlive() || e.isPassenger()) return;

        Vec3 from = e.position().add(0, e.getBbHeight() * 0.5, 0);
        Vec3 to = holder.position().add(0, holder.getBbHeight() * 0.5, 0);
        Vec3 delta = to.subtract(from);
        double dist = delta.length();

        if (dist > TELEPORT_DISTANCE) {
            if (e instanceof ServerPlayer sp) {
                sp.connection.teleport(holder.getX(), holder.getY(), holder.getZ(), sp.getYRot(), sp.getXRot());
            } else {
                e.moveTo(holder.getX(), holder.getY(), holder.getZ());
            }
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
            return;
        }

        if (dist > SLACK) {
            Vec3 dir = delta.scale(1.0 / dist);
            double pull = Math.min((dist - SLACK) * 0.08, 0.6);
            Vec3 v = e.getDeltaMovement().add(dir.scale(pull));
            double max = 1.6;
            if (v.lengthSqr() > max * max) {
                v = v.scale(max / v.length());
            }
            e.setDeltaMovement(v);
            e.hurtMarked = true; // для игроков: отправить клиенту новую скорость
        }
    }
}
