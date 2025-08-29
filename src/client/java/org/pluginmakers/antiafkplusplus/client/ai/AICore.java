package org.pluginmakers.antiafkplusplus.client.ai;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.FurnaceBlock;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AICore {
    private static final int SCAN_RADIUS = 16;
    private static final int DANGER_RADIUS = 12; // Extended combat range
    
    private final MinecraftClient client;
    private final Random random = ThreadLocalRandom.current();
    private long lastActionTime = 0;
    
    public enum AIState {
        IDLE, EXPLORING, COMBAT, EATING, CHEST_INTERACTION, FURNACE_MANAGEMENT, MINING
    }
    
    public AICore(MinecraftClient client) {
        this.client = client;
    }
    
    public AIDecision analyze() {
        if (client.player == null || client.world == null) {
            return new AIDecision(AIState.IDLE, null, null);
        }
        
        PlayerEntity player = client.player;
        World world = client.world;
        
        HostileEntity nearbyHostile = findNearbyHostile(world, player.getPos());
        if (nearbyHostile != null && nearbyHostile.distanceTo(player) < DANGER_RADIUS) {
            return new AIDecision(AIState.COMBAT, null, nearbyHostile);
        }
        
        if (shouldEat(player)) {
            return new AIDecision(AIState.EATING, null, null);
        }
        
        BlockPos furnacePos = findNearbyFurnace(world, player.getBlockPos());
        if (furnacePos != null && shouldCheckFurnace()) {
            return new AIDecision(AIState.FURNACE_MANAGEMENT, furnacePos, null);
        }
        
        BlockPos orePos = findNearbyOre(world, player.getBlockPos());
        if (orePos != null) {
            return new AIDecision(AIState.MINING, orePos, null);
        }
        
        BlockPos chestPos = findNearbyChest(world, player.getBlockPos());
        if (chestPos != null && random.nextFloat() < 0.15f) { // Reduced chest interaction frequency
            return new AIDecision(AIState.CHEST_INTERACTION, chestPos, null);
        }
        
        if (shouldMove()) {
            BlockPos explorePos = generateExploreTarget(player.getBlockPos());
            return new AIDecision(AIState.EXPLORING, explorePos, null);
        }
        
        return new AIDecision(AIState.IDLE, null, null);
    }
    
    private HostileEntity findNearbyHostile(World world, Vec3d playerPos) {
        return world.getEntitiesByClass(HostileEntity.class, 
            net.minecraft.util.math.Box.of(playerPos, DANGER_RADIUS * 2, DANGER_RADIUS * 2, DANGER_RADIUS * 2),
            entity -> entity.isAlive())
            .stream()
            .min(Comparator.comparingDouble(entity -> entity.distanceTo(client.player)))
            .orElse(null);
    }
    
    private boolean shouldEat(PlayerEntity player) {
        return player.getHungerManager().getFoodLevel() < 16 || player.getHealth() < player.getMaxHealth() * 0.7f;
    }
    
    private BlockPos findNearbyFurnace(World world, BlockPos center) {
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (world.getBlockState(pos).getBlock() instanceof FurnaceBlock) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
    
    private boolean shouldCheckFurnace() {
        return random.nextFloat() < 0.4f;
    }
    
    private BlockPos findNearbyOre(World world, BlockPos center) {
        if (!org.pluginmakers.antiafkplusplus.client.config.AntiAFKConfig.getInstance().onlyVisibleOres) {
            return findNearbyOreXray(world, center);
        }
        
        // Only find visible ores (exposed to air)
        List<Block> ores = getConfiguredOres();
        
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.add(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    
                    if (ores.contains(block) && isOreVisible(world, pos)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
    
    private BlockPos findNearbyOreXray(World world, BlockPos center) {
        List<Block> ores = getConfiguredOres();
        
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (ores.contains(world.getBlockState(pos).getBlock())) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
    
    private List<Block> getConfiguredOres() {
        var config = org.pluginmakers.antiafkplusplus.client.config.AntiAFKConfig.getInstance();
        List<Block> ores = new ArrayList<>();
        
        if (config.mineCoal) {
            ores.addAll(Arrays.asList(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE));
        }
        if (config.mineIron) {
            ores.addAll(Arrays.asList(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE));
        }
        if (config.mineGold) {
            ores.addAll(Arrays.asList(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE));
        }
        if (config.mineDiamond) {
            ores.addAll(Arrays.asList(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE));
        }
        if (config.mineEmerald) {
            ores.addAll(Arrays.asList(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE));
        }
        if (config.mineRedstone) {
            ores.addAll(Arrays.asList(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE));
        }
        
        return ores;
    }
    
    private boolean isOreVisible(World world, BlockPos orePos) {
        // Check if ore has at least one air block adjacent (visible)
        for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.values()) {
            BlockPos adjacent = orePos.offset(direction);
            if (world.getBlockState(adjacent).isAir()) {
                return true;
            }
        }
        return false;
    }
    
    private BlockPos findNearbyChest(World world, BlockPos center) {
        var basePerimeter = org.pluginmakers.antiafkplusplus.client.AntiAFKManager.getInstance().getBasePerimeter();
        
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (world.getBlockState(pos).getBlock() instanceof ChestBlock && 
                        basePerimeter.canInteractWithChest(pos)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
    
    private boolean shouldMove() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime > 8000 + random.nextInt(12000)) { // Less frequent movement for more natural behavior
            lastActionTime = currentTime;
            return true;
        }
        return false;
    }
    
    private BlockPos generateExploreTarget(BlockPos center) {
        int range = 8 + random.nextInt(16);
        int x = center.getX() + (random.nextInt(range * 2) - range);
        int z = center.getZ() + (random.nextInt(range * 2) - range);
        int y = center.getY() + (random.nextInt(6) - 3);
        return new BlockPos(x, y, z);
    }
    
    public static class AIDecision {
        public final AIState state;
        public final BlockPos targetPos;
        public final Entity targetEntity;
        
        public AIDecision(AIState state, BlockPos targetPos, Entity targetEntity) {
            this.state = state;
            this.targetPos = targetPos;
            this.targetEntity = targetEntity;
        }
    }
}