package org.pluginmakers.antiafkplusplus.client.ai;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.concurrent.ThreadLocalRandom;

public class ActionExecutor {
    private final MinecraftClient client;
    private long lastActionTime = 0;
    private static final int ACTION_COOLDOWN = 2000; // More natural timing
    
    public ActionExecutor(MinecraftClient client) {
        this.client = client;
    }
    
    public void executeAction(AICore.AIDecision decision) {
        if (!canPerformAction()) return;
        
        switch (decision.state) {
            case COMBAT -> executeCombat(decision.targetEntity);
            case EATING -> executeEating();
            case CHEST_INTERACTION -> executeChestInteraction(decision.targetPos);
            case FURNACE_MANAGEMENT -> executeFurnaceManagement(decision.targetPos);
            case MINING -> executeMining(decision.targetPos);
        }
        
        lastActionTime = System.currentTimeMillis();
    }
    
    private boolean canPerformAction() {
        return System.currentTimeMillis() - lastActionTime > ACTION_COOLDOWN;
    }
    
    private void executeCombat(Entity target) {
        if (client.player == null) return;
        
        // Kill-aura: Find all hostile entities in range
        java.util.List<Entity> hostileEntities = client.world.getOtherEntities(client.player, 
            client.player.getBoundingBox().expand(8.0), // Extended range
            entity -> entity instanceof HostileEntity && entity.isAlive());
        
        if (hostileEntities.isEmpty()) return;
        
        // Attack closest hostile
        Entity closestHostile = hostileEntities.stream()
            .min((e1, e2) -> Double.compare(
                client.player.distanceTo(e1), 
                client.player.distanceTo(e2)))
            .orElse(null);
        
        if (closestHostile == null) return;
        
        ClientPlayerEntity player = client.player;
        double distance = player.distanceTo(closestHostile);
        
        if (distance > 8.0) return;
        
        // Look at the target
        lookAtEntity(closestHostile);
        
        // Select weapon if available
        selectBestWeapon();
        
        // Attack with timing variation
        if (ThreadLocalRandom.current().nextFloat() < 0.8f) {
            attackEntity(closestHostile);
        }
        
        // Defensive movement
        if (distance < 3.0 && ThreadLocalRandom.current().nextFloat() < 0.5f) {
            performDodgeMovement();
        }
    }
    
    private void executeEating() {
        if (client.player == null) return;
        
        ItemStack foodItem = findFoodInInventory();
        if (foodItem.isEmpty()) return;
        
        // Switch to food item
        int foodSlot = findItemSlot(foodItem);
        if (foodSlot != -1 && foodSlot < 9) {
            if (foodSlot < 9) {
                try {
                    java.lang.reflect.Field field = client.player.getInventory().getClass().getDeclaredField("selectedSlot");
                    field.setAccessible(true);
                    field.setInt(client.player.getInventory(), foodSlot);
                } catch (Exception e) {}
            }
            
            // Start eating
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        }
    }
    
    private void executeChestInteraction(BlockPos chestPos) {
        if (client.player == null || client.world == null) return;
        
        double distance = client.player.getPos().distanceTo(Vec3d.ofCenter(chestPos));
        if (distance > 4.0) return;
        
        // Look at chest
        lookAtBlock(chestPos);
        
        // Right-click to open
        BlockHitResult hitResult = new BlockHitResult(
            Vec3d.ofCenter(chestPos), 
            Direction.UP, 
            chestPos, 
            false
        );
        
        client.interactionManager.interactBlock(
            client.player, 
            Hand.MAIN_HAND, 
            hitResult
        );
        
        // Enhanced chest interaction with item management
        scheduleEnhancedChestBrowsing();
    }
    
    private void executeFurnaceManagement(BlockPos furnacePos) {
        if (client.player == null) return;
        
        double distance = client.player.getPos().distanceTo(Vec3d.ofCenter(furnacePos));
        if (distance > 4.0) return;
        
        lookAtBlock(furnacePos);
        
        BlockHitResult hitResult = new BlockHitResult(
            Vec3d.ofCenter(furnacePos), 
            Direction.UP, 
            furnacePos, 
            false
        );
        
        client.interactionManager.interactBlock(
            client.player, 
            Hand.MAIN_HAND, 
            hitResult
        );
    }
    
    private void executeMining(BlockPos orePos) {
        if (client.player == null || client.interactionManager == null) return;
        
        double distance = client.player.getPos().distanceTo(Vec3d.ofCenter(orePos));
        if (distance > 5.0) return;
        
        // Select best tool
        selectBestTool(orePos);
        
        // Look at ore
        lookAtBlock(orePos);
        
        // Start mining
        BlockHitResult hitResult = new BlockHitResult(
            Vec3d.ofCenter(orePos), 
            Direction.UP, 
            orePos, 
            false
        );
        
        client.interactionManager.attackBlock(orePos, Direction.UP);
    }
    
    private void lookAtEntity(Entity entity) {
        if (client.player == null) return;
        
        Vec3d playerEyes = client.player.getEyePos();
        Vec3d targetPos = entity.getPos().add(0, entity.getHeight() * 0.5, 0);
        Vec3d direction = targetPos.subtract(playerEyes).normalize();
        
        float yaw = (float) (Math.atan2(direction.x, direction.z) * 180.0 / Math.PI);
        float pitch = (float) (Math.asin(-direction.y) * 180.0 / Math.PI);
        
        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
    }
    
    private void lookAtBlock(BlockPos pos) {
        if (client.player == null) return;
        
        Vec3d playerEyes = client.player.getEyePos();
        Vec3d targetPos = Vec3d.ofCenter(pos);
        Vec3d direction = targetPos.subtract(playerEyes).normalize();
        
        float yaw = (float) (Math.atan2(direction.x, direction.z) * 180.0 / Math.PI);
        float pitch = (float) (Math.asin(-direction.y) * 180.0 / Math.PI);
        
        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
    }
    
    private void selectBestWeapon() {
        if (client.player == null) return;
        
        int bestSlot = -1;
        float bestDamage = 0;
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                // Simplified weapon detection
                String itemName = stack.getItem().toString().toLowerCase();
                if (itemName.contains("sword") || itemName.contains("axe")) {
                    float damage = getItemDamage(stack);
                    if (damage > bestDamage) {
                        bestDamage = damage;
                        bestSlot = i;
                    }
                }
            }
        }
        
        if (bestSlot != -1 && bestSlot < 9) {
            try {
                java.lang.reflect.Field field = client.player.getInventory().getClass().getDeclaredField("selectedSlot");
                field.setAccessible(true);
                field.setInt(client.player.getInventory(), bestSlot);
            } catch (Exception e) {}
        }
    }
    
    private void selectBestTool(BlockPos pos) {
        if (client.player == null || client.world == null) return;
        
        String blockName = client.world.getBlockState(pos).getBlock().toString().toLowerCase();
        
        int bestSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String itemName = stack.getItem().toString().toLowerCase();
                
                if ((blockName.contains("ore") || blockName.contains("stone")) && itemName.contains("pickaxe")) {
                    bestSlot = i;
                    break;
                } else if (blockName.contains("wood") && itemName.contains("axe")) {
                    bestSlot = i;
                    break;
                } else if (blockName.contains("dirt") && itemName.contains("shovel")) {
                    bestSlot = i;
                    break;
                }
            }
        }
        
        if (bestSlot != -1 && bestSlot < 9) {
            try {
                java.lang.reflect.Field field = client.player.getInventory().getClass().getDeclaredField("selectedSlot");
                field.setAccessible(true);
                field.setInt(client.player.getInventory(), bestSlot);
            } catch (Exception e) {}
        }
    }
    
    private ItemStack findFoodInInventory() {
        if (client.player == null) return ItemStack.EMPTY;
        
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem().getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD)) {
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
    
    private void attackEntity(Entity target) {
        if (client.interactionManager == null) return;
        
        EntityHitResult hitResult = new EntityHitResult(target);
        client.interactionManager.attackEntity(client.player, target);
    }
    
    private void performDodgeMovement() {
        if (client.player == null) return;
        
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float dodgeDirection = random.nextFloat() * 360;
        
        Vec3d dodgeVelocity = new Vec3d(
            Math.cos(Math.toRadians(dodgeDirection)) * 0.3,
            0,
            Math.sin(Math.toRadians(dodgeDirection)) * 0.3
        );
        client.player.setVelocity(dodgeVelocity);
        
        // Schedule to stop dodge movement
        new Thread(() -> {
            try {
                Thread.sleep(200 + random.nextInt(300));
                if (client.player != null) {
                    client.player.setVelocity(Vec3d.ZERO);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void scheduleEnhancedChestBrowsing() {
        new Thread(() -> {
            try {
                Thread.sleep(1500 + ThreadLocalRandom.current().nextInt(2500));
                
                if (client.currentScreen != null && client.player != null) {
                    // Take armor if player has none equipped
                    takeArmorFromChest();
                    
                    // Take tools and weapons if inventory lacks them
                    takeToolsFromChest();
                    
                    // Take smelting materials
                    takeSmeltingMaterials();
                    
                    // Organize inventory
                    organizeInventory();
                    
                    Thread.sleep(500 + ThreadLocalRandom.current().nextInt(1000));
                    client.player.closeHandledScreen();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void takeArmorFromChest() {
        if (client.currentScreen == null || client.player == null) return;
        
        // Check if player has armor equipped (slots 36-39 are armor slots)
        boolean needsHelmet = client.player.getInventory().getStack(39).isEmpty(); // Helmet
        boolean needsChestplate = client.player.getInventory().getStack(38).isEmpty(); // Chestplate
        boolean needsLeggings = client.player.getInventory().getStack(37).isEmpty(); // Leggings
        boolean needsBoots = client.player.getInventory().getStack(36).isEmpty(); // Boots
        
        if (!needsHelmet && !needsChestplate && !needsLeggings && !needsBoots) return;
        
        // Search chest for armor and take it
        for (int i = 0; i < 27; i++) { // Chest slots
            try {
                ItemStack stack = client.player.currentScreenHandler.getSlot(i).getStack();
                if (!stack.isEmpty()) {
                    String itemName = stack.getItem().toString().toLowerCase();
                    
                    if ((needsHelmet && itemName.contains("helmet")) ||
                        (needsChestplate && itemName.contains("chestplate")) ||
                        (needsLeggings && itemName.contains("leggings")) ||
                        (needsBoots && itemName.contains("boots"))) {
                        
                        client.interactionManager.clickSlot(
                            client.player.currentScreenHandler.syncId,
                            i, 0, SlotActionType.QUICK_MOVE, client.player
                        );
                        Thread.sleep(200);
                    }
                }
            } catch (Exception e) {}
        }
    }
    
    private void takeToolsFromChest() {
        if (client.currentScreen == null || client.player == null) return;
        
        boolean hasPickaxe = hasToolInInventory("pickaxe");
        boolean hasSword = hasToolInInventory("sword");
        boolean hasAxe = hasToolInInventory("axe");
        
        for (int i = 0; i < 27; i++) {
            try {
                ItemStack stack = client.player.currentScreenHandler.getSlot(i).getStack();
                if (!stack.isEmpty()) {
                    String itemName = stack.getItem().toString().toLowerCase();
                    
                    if ((!hasPickaxe && itemName.contains("pickaxe")) ||
                        (!hasSword && itemName.contains("sword")) ||
                        (!hasAxe && itemName.contains("axe"))) {
                        
                        client.interactionManager.clickSlot(
                            client.player.currentScreenHandler.syncId,
                            i, 0, SlotActionType.QUICK_MOVE, client.player
                        );
                        Thread.sleep(200);
                        
                        if (itemName.contains("pickaxe")) hasPickaxe = true;
                        if (itemName.contains("sword")) hasSword = true;
                        if (itemName.contains("axe")) hasAxe = true;
                    }
                }
            } catch (Exception e) {}
        }
    }
    
    private void takeSmeltingMaterials() {
        if (client.currentScreen == null || client.player == null) return;
        
        for (int i = 0; i < 27; i++) {
            try {
                ItemStack stack = client.player.currentScreenHandler.getSlot(i).getStack();
                if (!stack.isEmpty()) {
                    String itemName = stack.getItem().toString().toLowerCase();
                    
                    if (itemName.contains("ore") || itemName.contains("raw_") || 
                        itemName.contains("coal") || itemName.contains("charcoal")) {
                        
                        client.interactionManager.clickSlot(
                            client.player.currentScreenHandler.syncId,
                            i, 0, SlotActionType.QUICK_MOVE, client.player
                        );
                        Thread.sleep(150);
                    }
                }
            } catch (Exception e) {}
        }
    }
    
    private void organizeInventory() {
        if (client.player == null) return;
        
        // Simple organization: move tools to hotbar
        for (int i = 9; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String itemName = stack.getItem().toString().toLowerCase();
                
                if (itemName.contains("sword") || itemName.contains("pickaxe") || 
                    itemName.contains("axe") || itemName.contains("shovel")) {
                    
                    // Find empty hotbar slot
                    for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
                        if (client.player.getInventory().getStack(hotbarSlot).isEmpty()) {
                            try {
                                client.interactionManager.clickSlot(
                                    client.player.currentScreenHandler.syncId,
                                    i, 0, SlotActionType.PICKUP, client.player
                                );
                                client.interactionManager.clickSlot(
                                    client.player.currentScreenHandler.syncId,
                                    hotbarSlot + 36, 0, SlotActionType.PICKUP, client.player
                                );
                                Thread.sleep(100);
                                break;
                            } catch (Exception e) {}
                        }
                    }
                }
            }
        }
    }
    
    private boolean hasToolInInventory(String toolType) {
        if (client.player == null) return false;
        
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem().toString().toLowerCase().contains(toolType)) {
                return true;
            }
        }
        return false;
    }
    
    private float getItemDamage(ItemStack stack) {
        // Simplified damage calculation
        String itemName = stack.getItem().toString().toLowerCase();
        if (itemName.contains("diamond")) return 7.0f;
        if (itemName.contains("iron")) return 6.0f;
        if (itemName.contains("stone")) return 5.0f;
        if (itemName.contains("wood")) return 4.0f;
        return 1.0f;
    }
}