package com.example.crackwhip;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WhipItem extends Item {
    /** 3 + базовый 1 = 4 урона (2 сердца). */
    public static final double MELEE_BONUS_DAMAGE = 3.0;
    public static final float LASH_DAMAGE = 4.0F;
    public static final double LASH_RANGE = 8.0;
    public static final int LASH_COOLDOWN = 14;

    public WhipItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, MELEE_BONUS_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                // длинный хлыст: +2 блока к дальности обычной атаки
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath(CrackWhip.MODID, "whip_reach"),
                                2.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static void playCrack(Level level, double x, double y, double z) {
        float pitch = 0.95F + level.random.nextFloat() * 0.1F;
        level.playSound(null, x, y, z, ModSounds.WHIP_CRACK.get(), SoundSource.PLAYERS, 1.0F, pitch);
    }

    /** Обычный удар (ЛКМ) тоже щёлкает. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        playCrack(target.level(), target.getX(), target.getY(), target.getZ());
        return true;
    }

    /** ПКМ — щелчок кнутом на расстоянии до 8 блоков. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }
        player.getCooldowns().addCooldown(this, LASH_COOLDOWN);
        player.swing(hand);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            playCrack(level, player.getX(), player.getEyeY(), player.getZ());

            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 end = eye.add(look.scale(LASH_RANGE));
            BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            double maxDist = blockHit.getType() == HitResult.Type.MISS
                    ? LASH_RANGE : blockHit.getLocation().distanceTo(eye);
            Vec3 reach = eye.add(look.scale(maxDist));
            AABB box = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.0);

            EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, reach, box,
                    e -> !e.isSpectator() && e.isPickable(), maxDist * maxDist);

            if (hit != null && hit.getEntity() instanceof LivingEntity target) {
                boolean hurt = target.hurt(level.damageSources().playerAttack(player), LASH_DAMAGE);
                if (hurt) {
                    target.knockback(0.4, player.getX() - target.getX(), player.getZ() - target.getZ());
                    target.hurtMarked = true;
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                            8, 0.25, 0.25, 0.25, 0.2);
                    EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    stack.hurtAndBreak(1, player, slot);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.crackwhip.whip.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
