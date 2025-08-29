package org.pluginmakers.antiafkplusplus.client;

import net.minecraft.client.MinecraftClient;
import org.pluginmakers.antiafkplusplus.client.ai.AICore;
import org.pluginmakers.antiafkplusplus.client.ai.MovementController;
import org.pluginmakers.antiafkplusplus.client.ai.ActionExecutor;
import org.pluginmakers.antiafkplusplus.client.ai.EmergencyManager;
import org.pluginmakers.antiafkplusplus.client.ai.MovementLearner;
import org.pluginmakers.antiafkplusplus.client.ai.BasePerimeter;
import org.pluginmakers.antiafkplusplus.client.automation.AutomationManager;
import org.pluginmakers.antiafkplusplus.client.config.AntiAFKConfig;

import java.util.concurrent.ThreadLocalRandom;

public class AntiAFKManager {
    private static AntiAFKManager instance;
    
    private final MinecraftClient client;
    private final AICore aiCore;
    private final MovementController movementController;
    private final ActionExecutor actionExecutor;
    private final EmergencyManager emergencyManager;
    private final AutomationManager automationManager;
    private final MovementLearner movementLearner;
    private final BasePerimeter basePerimeter;
    private final AntiAFKConfig config;
    
    private boolean enabled = false;
    private long lastUpdateTime = 0;
    private static final int UPDATE_INTERVAL = 50;
    
    private AntiAFKManager() {
        this.client = MinecraftClient.getInstance();
        this.aiCore = new AICore(client);
        this.movementController = new MovementController(client);
        this.actionExecutor = new ActionExecutor(client);
        this.emergencyManager = new EmergencyManager(client);
        this.automationManager = new AutomationManager(client);
        this.movementLearner = new MovementLearner(client);
        this.basePerimeter = new BasePerimeter(client);
        this.config = AntiAFKConfig.getInstance();
    }
    
    public static AntiAFKManager getInstance() {
        if (instance == null) {
            instance = new AntiAFKManager();
        }
        return instance;
    }
    
    public void toggle() {
        enabled = !enabled;
        
        if (enabled) {
            // Stop learning when mod is enabled
            movementLearner.stopLearning();
        } else {
            // Start learning when mod is disabled
            stopAllActions();
            movementLearner.startLearning();
        }
        
        if (client.player != null) {
            String status = enabled ? "§aEnabled" : "§cDisabled";
            String patterns = enabled ? "" : " (Learning: " + movementLearner.getPatternCount() + " patterns)";
            client.player.sendMessage(
                net.minecraft.text.Text.literal("AntiAFK " + status + patterns), 
                false
            );
        }
    }
    
    public void update() {
        // Always record movement when disabled
        if (!enabled) {
            movementLearner.recordMovement();
            return;
        }
        
        if (client.player == null || client.world == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return;
        }
        lastUpdateTime = currentTime;
        
        try {
            // Handle emergencies first
            if (emergencyManager.handleEmergency()) {
                return; // Emergency action in progress
            }
            
            // Get AI decision
            AICore.AIDecision decision = aiCore.analyze();
            
            // Execute movement if needed
            if (decision.targetPos != null) {
                movementController.moveTowards(decision.targetPos);
            } else if (decision.state == AICore.AIState.EXPLORING) {
                // Use learned movement patterns 60% of the time
                if (ThreadLocalRandom.current().nextFloat() < 0.6f && movementLearner.getPatternCount() > 0) {
                    movementLearner.applyLearnedMovement(movementController);
                } else {
                    movementController.randomWalk();
                }
            } else if (!movementController.isMoving()) {
                // Use learned patterns for idle movement occasionally
                if (ThreadLocalRandom.current().nextFloat() < 0.3f && movementLearner.getPatternCount() > 0) {
                    movementLearner.applyLearnedMovement(movementController);
                } else {
                    performIdleMovement();
                }
            }
            
            // Execute actions
            actionExecutor.executeAction(decision);
            
            // Update automation systems
            automationManager.update();
            
            // Random chat messages occasionally
            if (config.enableRandomChat && ThreadLocalRandom.current().nextFloat() < config.chatFrequency) {
                sendRandomChatMessage();
            }
            
        } catch (Exception e) {
            // Silently handle errors to prevent crashes
            e.printStackTrace();
        }
    }
    
    private void performIdleMovement() {
        if (ThreadLocalRandom.current().nextFloat() < 0.1f) {
            // Small random movements
            if (client.player != null) {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                
                // Slight mouse movement
                float yawChange = (random.nextFloat() - 0.5f) * 2.0f;
                float pitchChange = (random.nextFloat() - 0.5f) * 1.0f;
                
                client.player.setYaw(client.player.getYaw() + yawChange);
                client.player.setPitch(Math.max(-90, Math.min(90, client.player.getPitch() + pitchChange)));
                
                // Occasional small step
                if (random.nextFloat() < 0.3f) {
                    // Simulate movement input for 1.21.8
                    if (random.nextBoolean()) {
                        client.player.setYaw(client.player.getYaw() + (random.nextFloat() - 0.5f) * 10);
                    }
                    
                    // Stop movement after short time
                    new Thread(() -> {
                        try {
                            Thread.sleep(100 + random.nextInt(200));
                            if (client.player != null) {
                                // Movement handled by AI
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            }
        }
    }
    
    private void sendRandomChatMessage() {
        if (client.player == null || config.chatMessages.length == 0) return;
        
        String message = config.chatMessages[ThreadLocalRandom.current().nextInt(config.chatMessages.length)];
        
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatMessage(message);
        }
    }
    
    private void stopAllActions() {
        movementController.stopMovement();
        automationManager.reset();
        
        if (client.player != null) {
            // Stop all movement - handled by movement controller
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        boolean wasEnabled = this.enabled;
        this.enabled = enabled;
        
        if (enabled && !wasEnabled) {
            // Switching from disabled to enabled
            movementLearner.stopLearning();
        } else if (!enabled && wasEnabled) {
            // Switching from enabled to disabled
            stopAllActions();
            movementLearner.startLearning();
        } else if (!enabled) {
            stopAllActions();
        }
    }
    
    public void emergencyStop() {
        enabled = false;
        stopAllActions();
        movementLearner.startLearning(); // Start learning after emergency stop
        
        if (client.player != null) {
            client.player.sendMessage(
                net.minecraft.text.Text.literal("§cAntiAFK Emergency Stop Activated!"), 
                false
            );
        }
    }
    
    public MovementLearner getMovementLearner() {
        return movementLearner;
    }
    
    public BasePerimeter getBasePerimeter() {
        return basePerimeter;
    }
    

}