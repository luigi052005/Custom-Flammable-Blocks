package me.luigi.customflammableblocks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CustomFlammableBlocks implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("FlammableBlocks");
    private Commands commands;
    public boolean enabled;

    // The configuration object loaded from disk
    public CustomFlammableBlocksConfig config;
    private final Map<Block, int[]> defaultFlammability = new HashMap<>();  // Store default flammability data from Minecraft

    @Override
    public void onInitialize() {
        Path configFolder = FabricLoader.getInstance().getConfigDir();
        config = CustomFlammableBlocksConfig.load(configFolder);
        enabled = config.enabled;

        // Initialize the default flammability data
        FlammableBlockRegistry registry = FlammableBlockRegistry.getDefaultInstance();

        // Populate defaultFlammability with existing registry values
        for (Block block : Registries.BLOCK) {
            FlammableBlockRegistry.Entry entry = registry.get(block);
            if (entry != null) {
                // Extract burn chance and spread chance from the Entry
                defaultFlammability.put(block, new int[]{entry.getBurnChance(), entry.getSpreadChance()});
            } else {
                // Non-flammable blocks get [0, 0]
                defaultFlammability.put(block, new int[]{0, 0});
            }
        }

        // Register commands
        commands = new Commands(this);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Only register commands on the logical server
            if (environment.dedicated || environment.integrated) {
                commands.register(dispatcher);
            }
        });

        if (enabled) {
            registerAllFlammable();
        }
        LOG.info("Flammable Blocks Initialized!");
    }

    public void registerFlammable(Block block) {
        FlammableBlockRegistry.getDefaultInstance().add(block, 5, 20);
    }

    public void resetFlammable(Block block) {
        // Retrieve the default flammability data from stored map
        int[] flammability = defaultFlammability.get(block);
        FlammableBlockRegistry.getDefaultInstance().add(block, flammability[0], flammability[1]);
    }

    public void registerAllFlammable() {
        for (String id : config.BURNABLE_BLOCKS) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) {
                LOG.error("Invalid block ID: {}", id);
                continue;
            }
            Block block = Registries.BLOCK.get(identifier);
            registerFlammable(block);
        }
    }

    public void resetAllFlammable() {
        for (Block block : Registries.BLOCK) {
            resetFlammable(block);
        }
    }

    public void reload() {
        registerAllFlammable();
    }
}


