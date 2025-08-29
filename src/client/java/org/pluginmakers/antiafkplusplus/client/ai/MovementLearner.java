package org.pluginmakers.antiafkplusplus.client.ai;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MovementLearner {
    private final MinecraftClient client;
    private final List<MovementPattern> learnedPatterns = new ArrayList<>();
    private final File dataFile;
    private final Gson gson = new Gson();
    
    private Vec3d lastPosition = Vec3d.ZERO;
    private long lastRecordTime = 0;
    private boolean isRecording = false;
    private MovementPattern currentPattern = null;
    
    private static final int MAX_PATTERNS = 50;
    private static final int MIN_PATTERN_LENGTH = 5;
    private static final long RECORD_INTERVAL = 100; // ms
    
    public MovementLearner(MinecraftClient client) {
        this.client = client;
        this.dataFile = new File(client.runDirectory, "config/antiafkplusplus_movement.json");
        loadPatterns();
    }
    
    public void startLearning() {
        if (client.player == null) return;
        
        isRecording = true;
        lastPosition = client.player.getPos();
        lastRecordTime = System.currentTimeMillis();
        currentPattern = new MovementPattern();
    }
    
    public void stopLearning() {
        if (isRecording && currentPattern != null && currentPattern.movements.size() >= MIN_PATTERN_LENGTH) {
            learnedPatterns.add(currentPattern);
            
            // Keep only the most recent patterns
            if (learnedPatterns.size() > MAX_PATTERNS) {
                learnedPatterns.remove(0);
            }
            
            savePatterns();
        }
        
        isRecording = false;
        currentPattern = null;
    }
    
    public void recordMovement() {
        if (!isRecording || client.player == null || currentPattern == null) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRecordTime < RECORD_INTERVAL) return;
        
        Vec3d currentPos = client.player.getPos();
        Vec3d movement = currentPos.subtract(lastPosition);
        
        // Only record if there's actual movement
        if (movement.lengthSquared() > 0.001) {
            MovementStep step = new MovementStep();
            step.deltaX = movement.x;
            step.deltaY = movement.y;
            step.deltaZ = movement.z;
            step.yaw = client.player.getYaw();
            step.pitch = client.player.getPitch();
            step.isJumping = !client.player.isOnGround();
            step.isSprinting = client.player.isSprinting();
            step.timeDelta = currentTime - lastRecordTime;
            
            currentPattern.movements.add(step);
            
            // Limit pattern length to prevent memory issues
            if (currentPattern.movements.size() > 100) {
                currentPattern.movements.remove(0);
            }
        }
        
        lastPosition = currentPos;
        lastRecordTime = currentTime;
    }
    
    public MovementPattern getRandomPattern() {
        if (learnedPatterns.isEmpty()) {
            return generateDefaultPattern();
        }
        
        return learnedPatterns.get(ThreadLocalRandom.current().nextInt(learnedPatterns.size()));
    }
    
    public void applyLearnedMovement(MovementController controller) {
        if (client.player == null) return;
        
        MovementPattern pattern = getRandomPattern();
        if (pattern.movements.isEmpty()) return;
        
        // Apply a random segment of the pattern
        int startIndex = ThreadLocalRandom.current().nextInt(Math.max(1, pattern.movements.size() - 10));
        int endIndex = Math.min(startIndex + 5 + ThreadLocalRandom.current().nextInt(10), pattern.movements.size());
        
        new Thread(() -> {
            try {
                for (int i = startIndex; i < endIndex && i < pattern.movements.size(); i++) {
                    if (client.player == null) break;
                    
                    MovementStep step = pattern.movements.get(i);
                    applyMovementStep(step);
                    
                    Thread.sleep(Math.max(50, Math.min(500, step.timeDelta)));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void applyMovementStep(MovementStep step) {
        if (client.player == null) return;
        
        // Apply movement with some randomization
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double noiseX = (random.nextDouble() - 0.5) * 0.1;
        double noiseZ = (random.nextDouble() - 0.5) * 0.1;
        
        Vec3d velocity = new Vec3d(
            step.deltaX * 5 + noiseX, // Scale up delta for velocity
            0,
            step.deltaZ * 5 + noiseZ
        );
        
        // Apply speed variation based on learned sprinting
        if (step.isSprinting && random.nextFloat() < 0.8f) {
            velocity = velocity.multiply(1.3);
        }
        
        client.player.setVelocity(velocity);
        
        // Apply rotation with smoothing
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();
        
        float targetYaw = step.yaw + (random.nextFloat() - 0.5f) * 10f;
        float targetPitch = step.pitch + (random.nextFloat() - 0.5f) * 5f;
        
        // Smooth rotation
        float yawDiff = targetYaw - currentYaw;
        float pitchDiff = targetPitch - currentPitch;
        
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        client.player.setYaw(currentYaw + yawDiff * 0.3f);
        client.player.setPitch(currentPitch + pitchDiff * 0.3f);
        
        // Apply jumping
        if (step.isJumping && client.player.isOnGround() && random.nextFloat() < 0.7f) {
            client.player.jump();
        }
    }
    
    private MovementPattern generateDefaultPattern() {
        MovementPattern pattern = new MovementPattern();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Generate a simple walking pattern
        for (int i = 0; i < 10; i++) {
            MovementStep step = new MovementStep();
            step.deltaX = (random.nextDouble() - 0.5) * 0.1;
            step.deltaZ = (random.nextDouble() - 0.5) * 0.1;
            step.yaw = random.nextFloat() * 360;
            step.pitch = (random.nextFloat() - 0.5f) * 30;
            step.timeDelta = 200 + random.nextInt(300);
            step.isJumping = random.nextFloat() < 0.1f;
            step.isSprinting = random.nextFloat() < 0.3f;
            
            pattern.movements.add(step);
        }
        
        return pattern;
    }
    
    private void savePatterns() {
        try {
            dataFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(learnedPatterns, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save movement patterns: " + e.getMessage());
        }
    }
    
    private void loadPatterns() {
        if (!dataFile.exists()) return;
        
        try (FileReader reader = new FileReader(dataFile)) {
            Type listType = new TypeToken<List<MovementPattern>>(){}.getType();
            List<MovementPattern> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                learnedPatterns.clear();
                learnedPatterns.addAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("Failed to load movement patterns: " + e.getMessage());
        }
    }
    
    public int getPatternCount() {
        return learnedPatterns.size();
    }
    
    public static class MovementPattern {
        public List<MovementStep> movements = new ArrayList<>();
    }
    
    public static class MovementStep {
        public double deltaX, deltaY, deltaZ;
        public float yaw, pitch;
        public boolean isJumping, isSprinting;
        public long timeDelta;
    }
}