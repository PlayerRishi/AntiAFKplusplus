package org.pluginmakers.antiafkplusplus.client.ai;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class BasePerimeter {
    private final MinecraftClient client;
    private final List<BlockPos> perimeterPoints = new ArrayList<>();
    private final Set<BlockPos> playerPlacedChests = new HashSet<>();
    private final Set<BlockPos> markedChests = new HashSet<>();
    private final File dataFile;
    private final Gson gson = new Gson();
    
    private BlockPos minCorner = null;
    private BlockPos maxCorner = null;
    private boolean isRecordingPerimeter = false;
    
    public BasePerimeter(MinecraftClient client) {
        this.client = client;
        this.dataFile = new File(client.runDirectory, "config/antiafkplusplus_base.json");
        loadData();
    }
    
    public void startPerimeterRecording() {
        isRecordingPerimeter = true;
        perimeterPoints.clear();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§aStarted perimeter recording. Walk around your base!"), false);
        }
    }
    
    public void recordPerimeterPoint() {
        if (!isRecordingPerimeter || client.player == null) return;
        
        BlockPos pos = client.player.getBlockPos();
        perimeterPoints.add(pos);
        
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§ePoint " + perimeterPoints.size() + " recorded"), false);
        }
    }
    
    public void finishPerimeterRecording() {
        if (!isRecordingPerimeter) return;
        
        isRecordingPerimeter = false;
        calculateBounds();
        saveData();
        
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§aPerimeter recorded! Base bounds: " + 
                (minCorner != null ? minCorner.toShortString() + " to " + maxCorner.toShortString() : "None")), false);
        }
    }
    
    public void recordPlayerPlacedChest(BlockPos pos) {
        playerPlacedChests.add(pos);
        saveData();
    }
    
    public void markChestInteractable(BlockPos pos) {
        markedChests.add(pos);
        saveData();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§aChest marked as interactable"), false);
        }
    }
    
    public boolean canInteractWithChest(BlockPos chestPos) {
        // Always allow interaction with marked chests
        if (markedChests.contains(chestPos)) return true;
        
        // Always allow interaction with player-placed chests
        if (playerPlacedChests.contains(chestPos)) return true;
        
        // Allow interaction if chest is within base perimeter
        return isWithinBase(chestPos);
    }
    
    public boolean isWithinBase(BlockPos pos) {
        if (minCorner == null || maxCorner == null) return false;
        
        return pos.getX() >= minCorner.getX() && pos.getX() <= maxCorner.getX() &&
               pos.getY() >= minCorner.getY() && pos.getY() <= maxCorner.getY() &&
               pos.getZ() >= minCorner.getZ() && pos.getZ() <= maxCorner.getZ();
    }
    
    private void calculateBounds() {
        if (perimeterPoints.isEmpty()) return;
        
        int minX = perimeterPoints.stream().mapToInt(BlockPos::getX).min().orElse(0);
        int maxX = perimeterPoints.stream().mapToInt(BlockPos::getX).max().orElse(0);
        int minY = perimeterPoints.stream().mapToInt(BlockPos::getY).min().orElse(0);
        int maxY = perimeterPoints.stream().mapToInt(BlockPos::getY).max().orElse(0);
        int minZ = perimeterPoints.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        int maxZ = perimeterPoints.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        
        minCorner = new BlockPos(minX, minY, minZ);
        maxCorner = new BlockPos(maxX, maxY, maxZ);
    }
    
    private void saveData() {
        try {
            dataFile.getParentFile().mkdirs();
            Map<String, Object> data = new HashMap<>();
            data.put("perimeterPoints", perimeterPoints);
            data.put("playerPlacedChests", playerPlacedChests);
            data.put("markedChests", markedChests);
            data.put("minCorner", minCorner);
            data.put("maxCorner", maxCorner);
            
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(data, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save base data: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadData() {
        if (!dataFile.exists()) return;
        
        try (FileReader reader = new FileReader(dataFile)) {
            Map<String, Object> data = gson.fromJson(reader, Map.class);
            if (data != null) {
                if (data.get("perimeterPoints") != null) {
                    List<Map<String, Double>> points = (List<Map<String, Double>>) data.get("perimeterPoints");
                    perimeterPoints.clear();
                    for (Map<String, Double> point : points) {
                        perimeterPoints.add(new BlockPos(
                            point.get("x").intValue(),
                            point.get("y").intValue(),
                            point.get("z").intValue()
                        ));
                    }
                }
                
                if (data.get("minCorner") != null) {
                    Map<String, Double> min = (Map<String, Double>) data.get("minCorner");
                    minCorner = new BlockPos(min.get("x").intValue(), min.get("y").intValue(), min.get("z").intValue());
                }
                
                if (data.get("maxCorner") != null) {
                    Map<String, Double> max = (Map<String, Double>) data.get("maxCorner");
                    maxCorner = new BlockPos(max.get("x").intValue(), max.get("y").intValue(), max.get("z").intValue());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load base data: " + e.getMessage());
        }
    }
    
    public boolean isRecording() {
        return isRecordingPerimeter;
    }
}