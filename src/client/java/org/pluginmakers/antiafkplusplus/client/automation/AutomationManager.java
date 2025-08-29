package org.pluginmakers.antiafkplusplus.client.automation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutomationManager {
    private final MinecraftClient client;
    private final List<BlockPos> trackedFurnaces = new ArrayList<>();
    private long lastFurnaceCheck = 0;
    private static final int FURNACE_CHECK_INTERVAL = 30000; // Check every 30 seconds for more natural behavior
    
    public AutomationManager(MinecraftClient client) {
        this.client = client;
    }
    
    public void update() {
        if (client.player == null || client.world == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastFurnaceCheck > FURNACE_CHECK_INTERVAL) {
            checkFurnaces();
            lastFurnaceCheck = currentTime;
        }
        
        handleScreenInteractions();
    }
    
    private void checkFurnaces() {
        if (client.world == null || client.player == null) return;
        
        BlockPos playerPos = client.player.getBlockPos();
        
        for (int x = -16; x <= 16; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -16; z <= 16; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = client.world.getBlockState(pos);
                    
                    if (state.getBlock() instanceof FurnaceBlock) {
                        if (!trackedFurnaces.contains(pos)) {
                            trackedFurnaces.add(pos);
                        }
                        
                        if (state.get(FurnaceBlock.LIT)) {
                            scheduleFurnaceCheck(pos);
                        }
                    }
                }
            }
        }
    }
    
    private void scheduleFurnaceCheck(BlockPos furnacePos) {
        new Thread(() -> {
            try {
                Thread.sleep(60000 + ThreadLocalRandom.current().nextInt(60000)); // Wait 1-2 minutes
                
                if (client.player != null && client.world != null) {
                    double distance = client.player.getPos().distanceTo(
                        net.minecraft.util.math.Vec3d.ofCenter(furnacePos)
                    );
                    
                    if (distance < 32) {
                        checkSpecificFurnace(furnacePos);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void checkSpecificFurnace(BlockPos pos) {
        if (client.world == null || client.player == null) return;
        
        BlockState state = client.world.getBlockState(pos);
        if (!(state.getBlock() instanceof FurnaceBlock)) return;
        
        if (!state.get(FurnaceBlock.LIT)) {
            double distance = client.player.getPos().distanceTo(
                net.minecraft.util.math.Vec3d.ofCenter(pos)
            );
            
            if (distance <= 4.0) {
                interactWithFurnace(pos);
            }
        }
    }
    
    private void interactWithFurnace(BlockPos pos) {
        if (client.interactionManager == null || client.player == null) return;
        
        net.minecraft.util.hit.BlockHitResult hitResult = new net.minecraft.util.hit.BlockHitResult(
            net.minecraft.util.math.Vec3d.ofCenter(pos),
            net.minecraft.util.math.Direction.UP,
            pos,
            false
        );
        
        client.interactionManager.interactBlock(
            client.player,
            net.minecraft.util.Hand.MAIN_HAND,
            hitResult
        );
    }
    
    private void handleScreenInteractions() {
        if (client.currentScreen == null || client.player == null) return;
        
        if (client.player.currentScreenHandler instanceof FurnaceScreenHandler furnaceHandler) {
            handleFurnaceScreen(furnaceHandler);
        }
    }
    
    private void handleFurnaceScreen(FurnaceScreenHandler handler) {
        ItemStack outputStack = handler.getSlot(2).getStack();
        
        if (!outputStack.isEmpty()) {
            takeFurnaceOutput(handler);
        }
        
        addFurnaceInputs(handler);
        scheduleFurnaceScreenClose();
    }
    
    private void takeFurnaceOutput(FurnaceScreenHandler handler) {
        if (client.interactionManager == null) return;
        
        client.interactionManager.clickSlot(
            handler.syncId,
            2,
            0,
            SlotActionType.QUICK_MOVE,
            client.player
        );
    }
    
    private void addFurnaceInputs(FurnaceScreenHandler handler) {
        if (client.interactionManager == null || client.player == null) return;
        
        ItemStack fuelStack = handler.getSlot(1).getStack();
        if (fuelStack.isEmpty() || fuelStack.getCount() < 8) {
            addFuelToFurnace(handler);
        }
        
        ItemStack inputStack = handler.getSlot(0).getStack();
        if (inputStack.isEmpty() || inputStack.getCount() < 8) {
            addSmeltableItems(handler);
        }
    }
    
    private void addFuelToFurnace(FurnaceScreenHandler handler) {
        if (client.interactionManager == null || client.player == null) return;
        
        for (int i = 3; i < handler.slots.size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (isFuel(stack)) {
                client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, client.player);
                client.interactionManager.clickSlot(handler.syncId, 1, 0, SlotActionType.PICKUP, client.player);
                break;
            }
        }
    }
    
    private void addSmeltableItems(FurnaceScreenHandler handler) {
        if (client.interactionManager == null || client.player == null) return;
        
        // First check player inventory
        for (int i = 3; i < handler.slots.size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (isSmeltable(stack)) {
                client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, client.player);
                client.interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.PICKUP, client.player);
                return;
            }
        }
        
        // If no smeltable items in inventory, check nearby chests
        checkNearbyChestsForSmeltables();
    }
    
    private void checkNearbyChestsForSmeltables() {
        if (client.world == null || client.player == null) return;
        
        BlockPos playerPos = client.player.getBlockPos();
        
        for (int x = -8; x <= 8; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = client.world.getBlockState(pos);
                    
                    if (state.getBlock().toString().toLowerCase().contains("chest")) {
                        var basePerimeter = org.pluginmakers.antiafkplusplus.client.AntiAFKManager.getInstance().getBasePerimeter();
                        if (basePerimeter.canInteractWithChest(pos)) {
                            scheduleChestInteractionForSmelting(pos);
                            return;
                        }
                    }
                }
            }
        }
    }
    
    private void scheduleChestInteractionForSmelting(BlockPos chestPos) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                
                if (client.player != null && client.world != null) {
                    double distance = client.player.getPos().distanceTo(
                        net.minecraft.util.math.Vec3d.ofCenter(chestPos)
                    );
                    
                    if (distance <= 6.0) {
                        // Move closer to chest if needed
                        if (distance > 4.0) {
                            // Simple pathfinding to chest
                            net.minecraft.util.math.Vec3d direction = 
                                net.minecraft.util.math.Vec3d.ofCenter(chestPos)
                                .subtract(client.player.getPos()).normalize();
                            client.player.setVelocity(direction.multiply(0.2));
                            Thread.sleep(2000);
                        }
                        
                        // Interact with chest
                        net.minecraft.util.hit.BlockHitResult hitResult = 
                            new net.minecraft.util.hit.BlockHitResult(
                                net.minecraft.util.math.Vec3d.ofCenter(chestPos),
                                net.minecraft.util.math.Direction.UP,
                                chestPos,
                                false
                            );
                        
                        client.interactionManager.interactBlock(
                            client.player,
                            net.minecraft.util.Hand.MAIN_HAND,
                            hitResult
                        );
                        
                        Thread.sleep(1500);
                        
                        // Take smelting materials from chest
                        takeSmeltingMaterialsFromChest();
                        
                        Thread.sleep(1000);
                        
                        if (client.currentScreen != null) {
                            client.player.closeHandledScreen();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void takeSmeltingMaterialsFromChest() {
        if (client.currentScreen == null || client.player == null) return;
        
        try {
            for (int i = 0; i < 27; i++) { // Chest slots
                ItemStack stack = client.player.currentScreenHandler.getSlot(i).getStack();
                if (!stack.isEmpty() && isSmeltable(stack)) {
                    client.interactionManager.clickSlot(
                        client.player.currentScreenHandler.syncId,
                        i, 0, SlotActionType.QUICK_MOVE, client.player
                    );
                    Thread.sleep(200);
                }
            }
        } catch (Exception e) {}
    }
    
    private boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        return stack.isOf(Items.COAL) ||
               stack.isOf(Items.CHARCOAL) ||
               stack.isOf(Items.LAVA_BUCKET) ||
               stack.isOf(Items.BLAZE_ROD);
    }
    
    private boolean isSmeltable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        String itemName = stack.getItem().toString().toLowerCase();
        return itemName.contains("raw_") ||
               itemName.contains("ore") ||
               itemName.contains("cobblestone") ||
               stack.isOf(Items.CACTUS);
    }
    
    private void scheduleFurnaceScreenClose() {
        new Thread(() -> {
            try {
                Thread.sleep(2000 + ThreadLocalRandom.current().nextInt(3000));
                
                if (client.currentScreen != null && client.player != null) {
                    client.execute(() -> client.player.closeHandledScreen());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public void reset() {
        trackedFurnaces.clear();
    }
}