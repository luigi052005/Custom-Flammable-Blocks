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

public class CustomFlammableBlocks implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("FlammableBlocks");
    private Commands commands;
    public boolean enabled;

    // The configuration object loaded from disk
    public CustomFlammableBlocksConfig config;

    @Override
    public void onInitialize() {
        Path configFolder = FabricLoader.getInstance().getConfigDir();
        config = CustomFlammableBlocksConfig.load(configFolder);
        enabled = config.enabled;

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

    public void removeFlammable(Block block) {
        FlammableBlockRegistry.getDefaultInstance().add(block, 0, 0);
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
        for (String id : config.BURNABLE_BLOCKS) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) {
                LOG.error("Invalid block ID: {}", id);
                continue;
            }
            Block block = Registries.BLOCK.get(identifier);
            removeFlammable(block);
        }
    }

    public void reload() {
        registerAllFlammable();
    }
}