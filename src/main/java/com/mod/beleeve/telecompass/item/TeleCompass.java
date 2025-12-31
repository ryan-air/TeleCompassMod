package com.mod.beleeve.telecompass.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;

public class TeleCompass extends Item {

    private static final int XP_COST = 5; // xp cost to actually use the item
    private static final int COOLDOWN_TICKS = 200; // custom cooldown (item can't be that broken)

    public TeleCompass(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();

            // Check shift first
            if (player.isShiftKeyDown()) {
                // Extract location data and store it
                tag.putDouble("savedX", player.getX());
                tag.putDouble("savedY", player.getY());
                tag.putDouble("savedZ", player.getZ());
                tag.putString("savedDim", level.dimension().location().toString());
                tag.putBoolean("hasLocation", true);

                player.displayClientMessage(Component.literal("§6Location saved! §7(" +
                        (int)player.getX() + ", " + (int)player.getY() + ", " + (int)player.getZ() + ")"), true);
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 1.0F, 1.0F);

                spawnParticles(level, player.position(), 20);

            } else {
                // tp code
                if (!tag.getBoolean("hasLocation")) {
                    player.displayClientMessage(Component.literal("§cNo location saved yet"), true);
                    return InteractionResultHolder.fail(stack);
                }

                // check if they have xp
                if (player.experienceLevel < XP_COST) {
                    player.displayClientMessage(Component.literal("§cNeed " + XP_COST + " levels to teleport"), true);
                    return InteractionResultHolder.fail(stack);
                }

                // cooldown
                if (player.getCooldowns().isOnCooldown(this)) {
                    player.displayClientMessage(Component.literal("§cTele compass on cooldown"), true);
                    return InteractionResultHolder.fail(stack);
                }

                double x = tag.getDouble("savedX");
                double y = tag.getDouble("savedY");
                double z = tag.getDouble("savedZ");
                String dimension = tag.getString("savedDim");
                spawnParticles(level, player.position(), 30);
                player.teleportTo(x, y, z);
                spawnParticles(level, new Vec3(x, y, z), 30);
                player.giveExperienceLevels(-XP_COST);
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 1.0F, 1.0F);

                player.displayClientMessage(Component.literal("§aWarped through space!"), true);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    private void spawnParticles(Level level, Vec3 pos, int count) {
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < count; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 2;
                double offsetY = level.random.nextDouble() * 2;
                double offsetZ = (level.random.nextDouble() - 0.5) * 2;

                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        1, 0, 0, 0, 0.1);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();

        tooltip.add(Component.literal("§7Right-click: Teleport to saved location"));
        tooltip.add(Component.literal("§7Shift + Right-click: Save current location"));
        tooltip.add(Component.literal("§7Cost: " + XP_COST + " XP levels"));
        tooltip.add(Component.literal("§7Cooldown: 10 seconds"));

        if (tag != null && tag.getBoolean("hasLocation")) {
            int x = (int)tag.getDouble("savedX");
            int y = (int)tag.getDouble("savedY");
            int z = (int)tag.getDouble("savedZ");
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§6Saved: §f" + x + ", " + y + ", " + z));
        } else {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§cNo location saved"));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("hasLocation");
    }
}