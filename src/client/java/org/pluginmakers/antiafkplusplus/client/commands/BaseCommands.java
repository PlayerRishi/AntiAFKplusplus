package org.pluginmakers.antiafkplusplus.client.commands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.pluginmakers.antiafkplusplus.client.AntiAFKManager;

public class BaseCommands {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static void handleCommand(String command) {
        var manager = AntiAFKManager.getInstance();
        var basePerimeter = manager.getBasePerimeter();
        
        String[] parts = command.toLowerCase().split(" ");
        
        switch (parts[0]) {
            case "startperimeter" -> {
                basePerimeter.startPerimeterRecording();
            }
            case "addpoint" -> {
                basePerimeter.recordPerimeterPoint();
            }
            case "finishperimeter" -> {
                basePerimeter.finishPerimeterRecording();
            }
            case "markchest" -> {
                if (client.player != null) {
                    BlockPos pos = client.player.getBlockPos();
                    // Find chest near player
                    for (int x = -3; x <= 3; x++) {
                        for (int y = -2; y <= 2; y++) {
                            for (int z = -3; z <= 3; z++) {
                                BlockPos chestPos = pos.add(x, y, z);
                                if (client.world != null && 
                                    client.world.getBlockState(chestPos).getBlock().toString().toLowerCase().contains("chest")) {
                                    basePerimeter.markChestInteractable(chestPos);
                                    return;
                                }
                            }
                        }
                    }
                    client.player.sendMessage(net.minecraft.text.Text.literal("§cNo chest found nearby"), false);
                }
            }
            default -> {
                if (client.player != null) {
                    client.player.sendMessage(net.minecraft.text.Text.literal(
                        "§eAntiAFK Base Commands:\n" +
                        "§7/antiafk startperimeter - Start recording base perimeter\n" +
                        "§7/antiafk addpoint - Add current position to perimeter\n" +
                        "§7/antiafk finishperimeter - Finish perimeter recording\n" +
                        "§7/antiafk markchest - Mark nearby chest as interactable"
                    ), false);
                }
            }
        }
    }
}