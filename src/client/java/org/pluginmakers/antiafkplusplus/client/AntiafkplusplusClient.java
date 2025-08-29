package org.pluginmakers.antiafkplusplus.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.pluginmakers.antiafkplusplus.client.config.AntiAFKConfig;
import org.pluginmakers.antiafkplusplus.client.commands.BaseCommands;
import com.mojang.brigadier.arguments.StringArgumentType;

public class AntiafkplusplusClient implements ClientModInitializer {
    private static KeyBinding toggleKey;
    
    @Override
    public void onInitializeClient() {
        AntiAFKConfig config = AntiAFKConfig.getInstance();
        
        // Register keybindings
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.antiafkplusplus.toggle",
            InputUtil.Type.KEYSYM,
            config.toggleKey,
            "category.antiafkplusplus"
        ));
        
        KeyBinding emergencyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.antiafkplusplus.emergency",
            InputUtil.Type.KEYSYM,
            config.emergencyStopKey,
            "category.antiafkplusplus"
        ));
        
        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("antiafk")
                .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                    .executes(context -> {
                        String command = StringArgumentType.getString(context, "command");
                        BaseCommands.handleCommand(command);
                        return 1;
                    }))
                .executes(context -> {
                    BaseCommands.handleCommand("help");
                    return 1;
                }));
        });
        
        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle keybindings
            while (toggleKey.wasPressed()) {
                AntiAFKManager.getInstance().toggle();
            }
            
            while (emergencyKey.wasPressed()) {
                AntiAFKManager.getInstance().emergencyStop();
            }
            
            // Update AI system
            AntiAFKManager.getInstance().update();
        });
    }
}
