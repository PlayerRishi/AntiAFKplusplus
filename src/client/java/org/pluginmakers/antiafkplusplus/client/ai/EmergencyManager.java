package org.pluginmakers.antiafkplusplus.client.ai;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.pluginmakers.antiafkplusplus.client.config.AntiAFKConfig;

public class EmergencyManager {
    private final MinecraftClient client;
    private final AntiAFKConfig config;
    private boolean clutchInProgress = false;
    private long lastClutchTime = 0;
    
    public EmergencyManager(MinecraftClient client) {
        this.client = client;
        this.config = AntiAFKConfig.getInstance();
    }
    
    public boolean handleEmergency() {
        if (!config.waterBucketClutch || client.player == null) return false;
        
        ClientPlayerEntity player = client.player;
        
        if (isFallingDangerously(player)) {
            return performWaterBucketClutch(player);
        }
        
        return false;
    }
    
    private boolean isFallingDangerously(ClientPlayerEntity player) {
        if (player.getVelocity().y < -0.5 && !player.isOnGround()) {
            Vec3d playerPos = player.getPos();
            BlockPos groundPos = findGroundBelow(playerPos);
            
            if (groundPos != null) {
                double fallDistance = playerPos.y - groundPos.getY();
                return fallDistance > config.clutchHeight;
            }
        }
        return false;
    }
    
    private BlockPos findGroundBelow(Vec3d playerPos) {
        if (client.world == null) return null;
        
        for (int y = (int)playerPos.y; y > client.world.getBottomY(); y--) {
            BlockPos pos = new BlockPos((int)playerPos.x, y, (int)playerPos.z);
            if (!client.world.getBlockState(pos).isAir()) {
                return pos;
            }
        }
        return null;
    }
    
    private boolean performWaterBucketClutch(ClientPlayerEntity player) {
        if (clutchInProgress || System.currentTimeMillis() - lastClutchTime < 5000) {
            return false;
        }
        
        ItemStack waterBucket = findWaterBucket(player);
        if (waterBucket.isEmpty()) return false;
        
        int bucketSlot = findItemSlot(waterBucket);
        if (bucketSlot == -1 || bucketSlot >= 9) return false;
        
        if (bucketSlot < 9) {
            try {
                java.lang.reflect.Field field = player.getInventory().getClass().getDeclaredField("selectedSlot");
                field.setAccessible(true);
                field.setInt(player.getInventory(), bucketSlot);
            } catch (Exception e) {}
        }
        player.setPitch(90.0f);
        
        Vec3d playerPos = player.getPos();
        BlockPos placePos = new BlockPos((int)playerPos.x, (int)playerPos.y - 2, (int)playerPos.z);
        
        BlockHitResult hitResult = new BlockHitResult(
            Vec3d.ofCenter(placePos),
            Direction.UP,
            placePos,
            false
        );
        
        if (client.interactionManager != null) {
            client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
            clutchInProgress = true;
            lastClutchTime = System.currentTimeMillis();
            return true;
        }
        
        return false;
    }
    
    private ItemStack findWaterBucket(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.WATER_BUCKET)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
    
    private int findItemSlot(ItemStack item) {
        if (client.player == null) return -1;
        
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            if (ItemStack.areItemsEqual(client.player.getInventory().getStack(i), item)) {
                return i;
            }
        }
        return -1;
    }
    
    public boolean isClutchInProgress() {
        return clutchInProgress;
    }
}