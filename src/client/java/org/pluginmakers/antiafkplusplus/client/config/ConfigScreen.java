package org.pluginmakers.antiafkplusplus.client.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final AntiAFKConfig config;
    
    public ConfigScreen(Screen parent) {
        super(Text.literal("AntiAFK++ Config"));
        this.parent = parent;
        this.config = AntiAFKConfig.getInstance();
    }
    
    @Override
    protected void init() {
        int y = 40;
        int spacing = 25;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Movement: " + (config.enableMovement ? "ON" : "OFF")), 
            button -> {
                config.enableMovement = !config.enableMovement;
                button.setMessage(Text.literal("Movement: " + (config.enableMovement ? "ON" : "OFF")));
            }).dimensions(20, y, 200, 20).build());
        y += spacing;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Combat: " + (config.enableCombat ? "ON" : "OFF")), 
            button -> {
                config.enableCombat = !config.enableCombat;
                button.setMessage(Text.literal("Combat: " + (config.enableCombat ? "ON" : "OFF")));
            }).dimensions(20, y, 200, 20).build());
        y += spacing;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Water Bucket Clutch: " + (config.waterBucketClutch ? "ON" : "OFF")), 
            button -> {
                config.waterBucketClutch = !config.waterBucketClutch;
                button.setMessage(Text.literal("Water Bucket Clutch: " + (config.waterBucketClutch ? "ON" : "OFF")));
            }).dimensions(20, y, 200, 20).build());
        y += spacing;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Mining: " + (config.enableMining ? "ON" : "OFF")), 
            button -> {
                config.enableMining = !config.enableMining;
                button.setMessage(Text.literal("Mining: " + (config.enableMining ? "ON" : "OFF")));
            }).dimensions(20, y, 200, 20).build());
        y += spacing;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Only Visible Ores: " + (config.onlyVisibleOres ? "ON" : "OFF")), 
            button -> {
                config.onlyVisibleOres = !config.onlyVisibleOres;
                button.setMessage(Text.literal("Only Visible Ores: " + (config.onlyVisibleOres ? "ON" : "OFF")));
            }).dimensions(20, y, 200, 20).build());
        y += spacing;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Furnace Management: " + (config.enableFurnaceManagement ? "ON" : "OFF")), 
            button -> {
                config.enableFurnaceManagement = !config.enableFurnaceManagement;
                button.setMessage(Text.literal("Furnace Management: " + (config.enableFurnaceManagement ? "ON" : "OFF")));
            }).dimensions(20, y, 200, 20).build());
        y += spacing;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Random Chat: " + (config.enableRandomChat ? "ON" : "OFF")), 
            button -> {
                config.enableRandomChat = !config.enableRandomChat;
                button.setMessage(Text.literal("Random Chat: " + (config.enableRandomChat ? "ON" : "OFF")));
            }).dimensions(20, y, 200, 20).build());
        y += spacing;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
            config.save();
        }).dimensions(width / 2 - 100, height - 50, 80, 20).build());
        
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> {
            config.save();
            client.setScreen(parent);
        }).dimensions(width / 2 + 20, height - 50, 80, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x88000000);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void close() {
        config.save();
        client.setScreen(parent);
    }
}