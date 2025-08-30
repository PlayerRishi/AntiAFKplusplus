# AntiAFK++

An advanced anti-AFK Minecraft mod with AI-driven behavior that makes your character appear naturally active.

## Features

### 🤖 AI-Driven Behavior
- **Environment Analysis**: Scans surroundings for threats, resources, and opportunities
- **Realistic Decision Making**: Prioritizes actions like a real player would
- **Adaptive Movement**: Natural pathfinding and exploration patterns

### ⚔️ Combat System
- **Automatic Threat Detection**: Identifies and engages hostile mobs
- **Smart Combat**: Uses best available weapons and defensive maneuvers
- **Health Management**: Automatically eats food when health/hunger is low

### 🏗️ Automation Features
- **Furnace Management**: Automatically checks and manages smelting operations
- **Resource Gathering**: Mines nearby ores and valuable blocks
- **Chest Interaction**: Browses and organizes storage containers
- **Smart Tool Selection**: Uses appropriate tools for different tasks

### 🚶 Movement & Exploration
- **Realistic Walking**: Natural movement patterns with slight randomization
- **Obstacle Navigation**: Handles jumping, climbing, and pathfinding
- **Area Exploration**: Discovers new areas while staying within safe bounds

### 💬 Social Features
- **Optional Chat Messages**: Sends realistic chat messages occasionally
- **Player Detection**: Can pause when other players are nearby (configurable)

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download AntiAFK++ and place in your `mods` folder
4. Launch Minecraft

## Usage

### Basic Controls
- **Toggle Key**: Press `K` to enable/disable AntiAFK
- **Status Messages**: Receive in-game notifications when toggling

### Configuration
The mod creates a config file at `.minecraft/config/antiafkplusplus.json` with these options:

```json
{
  "enableMovement": true,
  "movementRadius": 16,
  "enableCombat": true,
  "autoEat": true,
  "enableFurnaceManagement": true,
  "enableMining": true,
  "enableChestInteraction": true,
  "enableRandomChat": false,
  "pauseOnPlayerNearby": true
}
```

## AI Behavior Priority

1. **Combat** - Immediate threat response
2. **Health/Hunger** - Survival needs
3. **Automation** - Furnace/resource management
4. **Resource Gathering** - Mining and collection
5. **Exploration** - Area discovery and movement
6. **Idle Actions** - Subtle anti-AFK movements

## Safety Features

- **Player Detection**: Automatically pauses when other players are nearby
- **Emergency Health**: Stops risky actions when health is critically low
- **Configurable Limits**: Set movement radius and behavior boundaries
- **Error Handling**: Graceful failure recovery to prevent crashes

## Technical Details

### AI Architecture
- **AICore**: Central decision-making system
- **MovementController**: Handles pathfinding and navigation
- **ActionExecutor**: Manages combat, interaction, and tool usage
- **AutomationManager**: Oversees furnaces and resource processing

### Performance
- **Efficient Scanning**: Optimized environment analysis
- **Threaded Operations**: Non-blocking automation tasks
- **Memory Management**: Minimal resource usage

## Building from Source

```bash
git clone <repository>
cd AntiAFKplusplus
./gradlew build
```

## Compatibility

- **Minecraft**: 1.21.8
- **Fabric Loader**: 0.17.2+
- **Fabric API**: 0.132.0+

## Disclaimer

This mod is for educational and single-player use. Always follow server rules and terms of service. The authors are not responsible for any consequences of using this mod on multiplayer servers.

## License

Custom License - See LICENSE.txt for details
