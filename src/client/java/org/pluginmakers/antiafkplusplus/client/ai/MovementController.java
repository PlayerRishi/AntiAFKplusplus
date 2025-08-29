package org.pluginmakers.antiafkplusplus.client.ai;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.util.concurrent.ThreadLocalRandom;

public class MovementController {
    private final MinecraftClient client;
    private final PathFinder pathFinder;
    private BlockPos currentTarget;
    private long lastMovementTime = 0;
    private boolean isMoving = false;
    
    public MovementController(MinecraftClient client) {
        this.client = client;
        this.pathFinder = new PathFinder();
    }
    
    public void moveTowards(BlockPos target) {
        if (client.player == null || client.world == null) return;
        
        this.currentTarget = target;
        ClientPlayerEntity player = client.player;
        
        Vec3d playerPos = player.getPos();
        Vec3d targetPos = Vec3d.ofCenter(target);
        double distance = playerPos.distanceTo(targetPos);
        
        if (distance < 1.5) {
            stopMovement();
            return;
        }
        
        Vec3d direction = targetPos.subtract(playerPos).normalize();
        
        // Add some randomness for realistic movement
        direction = addMovementNoise(direction);
        
        // Set movement inputs
        setMovementInputs(direction);
        
        // Handle jumping and looking
        handleJumping(player);
        lookTowards(targetPos);
        
        isMoving = true;
        lastMovementTime = System.currentTimeMillis();
    }
    
    public void randomWalk() {
        if (client.player == null) return;
        
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // 30% chance to stand still for natural behavior
        if (random.nextFloat() < 0.3f) {
            standStill();
            return;
        }
        
        // 20% chance to jump in place
        if (random.nextFloat() < 0.2f) {
            client.player.jump();
            return;
        }
        
        BlockPos playerPos = client.player.getBlockPos();
        BlockPos randomTarget = generateRandomTarget(playerPos);
        moveTowards(randomTarget);
    }
    
    private void standStill() {
        if (client.player == null) return;
        
        stopMovement();
        
        // Random look around while standing
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float randomYaw = client.player.getYaw() + (random.nextFloat() - 0.5f) * 60f;
        float randomPitch = (random.nextFloat() - 0.5f) * 30f;
        
        client.player.setYaw(randomYaw);
        client.player.setPitch(randomPitch);
    }
    
    private Vec3d addMovementNoise(Vec3d direction) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double noiseX = (random.nextDouble() - 0.5) * 0.1;
        double noiseZ = (random.nextDouble() - 0.5) * 0.1;
        
        return direction.add(noiseX, 0, noiseZ).normalize();
    }
    
    private void setMovementInputs(Vec3d direction) {
        if (client.player == null) return;
        
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Vary movement speed - sometimes walk, sometimes run
        double speedMultiplier = random.nextFloat() < 0.6f ? 0.3 : 0.15; // 60% chance to run
        
        // Add sprint jumping occasionally
        if (random.nextFloat() < 0.1f && client.player.isOnGround()) {
            client.player.jump();
            speedMultiplier *= 1.5; // Faster when jumping
        }
        
        Vec3d velocity = new Vec3d(direction.x * speedMultiplier, 0, direction.z * speedMultiplier);
        if (Math.abs(direction.x) > 0.1 && Math.abs(direction.z) > 0.1) {
            velocity = velocity.multiply(0.8);
        }
        client.player.setVelocity(velocity);
    }
    
    private void handleJumping(ClientPlayerEntity player) {
        if (client.world == null) return;
        
        // Check if there's a block in front that requires jumping
        Vec3d lookDirection = player.getRotationVector();
        float yaw = player.getYaw();
        net.minecraft.util.math.Direction facing = net.minecraft.util.math.Direction.NORTH;
        int facingIndex = Math.round(yaw / 90.0f) & 3;
        switch (facingIndex) {
            case 0 -> facing = net.minecraft.util.math.Direction.SOUTH;
            case 1 -> facing = net.minecraft.util.math.Direction.WEST;
            case 2 -> facing = net.minecraft.util.math.Direction.NORTH;
            case 3 -> facing = net.minecraft.util.math.Direction.EAST;
        }
        BlockPos frontPos = player.getBlockPos().offset(facing);
        
        BlockState frontBlock = client.world.getBlockState(frontPos);
        BlockState aboveBlock = client.world.getBlockState(frontPos.up());
        
        if (!frontBlock.isAir() && aboveBlock.isAir() && player.isOnGround()) {
            client.player.jump();
        }
    }
    
    private void lookTowards(Vec3d target) {
        if (client.player == null) return;
        
        Vec3d playerPos = client.player.getEyePos();
        Vec3d direction = target.subtract(playerPos).normalize();
        
        // Calculate yaw and pitch
        float yaw = (float) (Math.atan2(direction.x, direction.z) * 180.0 / Math.PI);
        float pitch = (float) (Math.asin(-direction.y) * 180.0 / Math.PI);
        
        // Add slight randomness to look more natural
        ThreadLocalRandom random = ThreadLocalRandom.current();
        yaw += (random.nextFloat() - 0.5f) * 5.0f;
        pitch += (random.nextFloat() - 0.5f) * 3.0f;
        
        // Smoothly interpolate to new rotation
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();
        
        float yawDiff = yaw - currentYaw;
        float pitchDiff = pitch - currentPitch;
        
        // Normalize yaw difference
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        // Apply smooth rotation
        float rotationSpeed = 0.1f;
        client.player.setYaw(currentYaw + yawDiff * rotationSpeed);
        client.player.setPitch(currentPitch + pitchDiff * rotationSpeed);
    }
    
    private BlockPos generateRandomTarget(BlockPos center) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int range = 8 + random.nextInt(16); // Larger movement range
        
        int x = center.getX() + (random.nextInt(range * 2) - range);
        int z = center.getZ() + (random.nextInt(range * 2) - range);
        int y = center.getY();
        
        // Find safe ground level
        if (client.world != null) {
            for (int checkY = y + 5; checkY > y - 15; checkY--) {
                BlockPos checkPos = new BlockPos(x, checkY, z);
                if (!client.world.getBlockState(checkPos).isAir() && 
                    client.world.getBlockState(checkPos.up()).isAir() &&
                    client.world.getBlockState(checkPos.up(2)).isAir()) {
                    return checkPos.up();
                }
            }
        }
        
        return new BlockPos(x, y, z);
    }
    
    public void stopMovement() {
        if (client.player == null) return;
        
        client.player.setVelocity(Vec3d.ZERO);
        isMoving = false;
    }
    
    public boolean isMoving() {
        return isMoving && (System.currentTimeMillis() - lastMovementTime < 100);
    }
    
    public boolean hasReachedTarget() {
        if (currentTarget == null || client.player == null) return true;
        
        return client.player.getPos().distanceTo(Vec3d.ofCenter(currentTarget)) < 2.0;
    }
    
    public void applyLearnedPattern(java.util.List<org.pluginmakers.antiafkplusplus.client.ai.MovementLearner.MovementStep> pattern) {
        if (client.player == null || pattern.isEmpty()) return;
        
        // Apply the first few steps of the pattern with current position as base
        int stepsToApply = Math.min(3, pattern.size());
        
        for (int i = 0; i < stepsToApply; i++) {
            var step = pattern.get(i);
            
            // Apply movement with scaling
            Vec3d velocity = new Vec3d(step.deltaX * 3, 0, step.deltaZ * 3);
            client.player.setVelocity(velocity);
            
            // Apply rotation smoothly
            float yawDiff = step.yaw - client.player.getYaw();
            while (yawDiff > 180) yawDiff -= 360;
            while (yawDiff < -180) yawDiff += 360;
            
            client.player.setYaw(client.player.getYaw() + yawDiff * 0.2f);
            client.player.setPitch(client.player.getPitch() + (step.pitch - client.player.getPitch()) * 0.2f);
            
            // Apply jumping if learned
            if (step.isJumping && client.player.isOnGround() && ThreadLocalRandom.current().nextFloat() < 0.8f) {
                client.player.jump();
            }
            
            break; // Only apply first step per call for smooth movement
        }
        
        isMoving = true;
        lastMovementTime = System.currentTimeMillis();
    }
    
    private static class PathFinder {
        // Simplified pathfinding - can be expanded later
        public boolean canReach(World world, BlockPos from, BlockPos to) {
            // Basic line-of-sight check
            return true; // Simplified for now
        }
    }
}