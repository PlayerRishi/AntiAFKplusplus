package org.pluginmakers.antiafkplusplus.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class AntiAFKConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("antiafkplusplus.json");
    
    // Keybinds
    public int toggleKey = GLFW.GLFW_KEY_K;
    public int emergencyStopKey = GLFW.GLFW_KEY_P;
    
    // Movement
    public boolean enableMovement = true;
    public int movementRadius = 16;
    public int explorationRange = 32;
    public int movementInterval = 5000;
    public float movementSpeed = 0.8f;
    
    // Combat
    public boolean enableCombat = true;
    public int combatRange = 8;
    public boolean autoEat = true;
    public float healthThreshold = 0.7f;
    public int hungerThreshold = 16;
    public boolean waterBucketClutch = true;
    public float clutchHeight = 10.0f;
    
    // Automation
    public boolean enableFurnaceManagement = true;
    public boolean autoFuelFurnaces = true;
    public boolean autoSmeltItems = true;
    public boolean autoCollectOutput = true;
    public int furnaceCheckInterval = 30;
    
    public boolean enableMining = true;
    public boolean mineCoal = true;
    public boolean mineIron = true;
    public boolean mineGold = true;
    public boolean mineDiamond = true;
    public boolean mineEmerald = true;
    public boolean mineRedstone = true;
    public boolean onlyVisibleOres = true;
    
    public boolean enableChestInteraction = true;
    public float chestInteractionChance = 0.3f;
    public boolean organizeChests = false;
    
    // Chat
    public boolean enableRandomChat = false;
    public float chatFrequency = 0.0001f;
    public String[] chatMessages = {
        "nice view here",
        "hmm", 
        "interesting",
        "checking things out",
        "exploring a bit",
        "looking for resources",
        "just mining around"
    };
    
    // Safety
    public boolean pauseOnPlayerNearby = true;
    public int playerDetectionRadius = 32;
    public boolean pauseOnLowHealth = true;
    public float emergencyHealthThreshold = 0.3f;
    public boolean pauseInDanger = true;
    
    private static AntiAFKConfig instance;
    
    public static AntiAFKConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }
    
    public static AntiAFKConfig load() {
        try {
            if (CONFIG_PATH.toFile().exists()) {
                try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                    return GSON.fromJson(reader, AntiAFKConfig.class);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load AntiAFK config: " + e.getMessage());
        }
        
        AntiAFKConfig config = new AntiAFKConfig();
        config.save();
        return config;
    }
    
    public void save() {
        try {
            CONFIG_PATH.getParent().toFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save AntiAFK config: " + e.getMessage());
        }
    }
}