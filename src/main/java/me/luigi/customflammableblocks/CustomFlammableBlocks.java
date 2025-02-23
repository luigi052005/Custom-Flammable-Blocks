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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomFlammableBlocks implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("FlammableBlocks");
    private Commands commands;
    public List<FlammableBlockEntry> BURNABLE_BLOCKS;
    public boolean enabled;

    // Backup of Minecraft’s default flammability settings
    private final Map<String, FlammableBlockEntry> defaultFlammability = new HashMap<>();

    // The configuration object loaded from disk
    public CustomFlammableBlocksConfig config;

    @Override
    public void onInitialize() {
        Path configFolder = FabricLoader.getInstance().getConfigDir();
        config = CustomFlammableBlocksConfig.load(configFolder);
        BURNABLE_BLOCKS = config.BURNABLE_BLOCKS;
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

        // Backup defaults for each block that has flammability
        for (Identifier id : Registries.BLOCK.getIds()) {
            Block block = Registries.BLOCK.get(id);
            FlammableBlockRegistry.Entry entry = FlammableBlockRegistry.getDefaultInstance().get(block);
            if (entry != null) {
                defaultFlammability.put(id.toString(),
                        new FlammableBlockEntry(id.toString(), entry.getBurnChance(), entry.getSpreadChance()));
            }
        }
        LOG.info("Flammable Blocks Initialized!");
    }

    public void registerFlammable(Block block, int burnChance, int spreadChance) {
        FlammableBlockRegistry.getDefaultInstance().add(block, burnChance, spreadChance);
    }

    public void registerAllFlammable() {
        for (FlammableBlockEntry entry : BURNABLE_BLOCKS) {
            Identifier identifier = Identifier.tryParse(entry.blockId);
            int burnChange = entry.burnChance;
            int spreadChange = entry.spreadChance;
            if (identifier == null) {
                LOG.error("Invalid block ID: {}", entry.blockId);
                continue;
            }
            Block block = Registries.BLOCK.get(identifier);
            registerFlammable(block, burnChange, spreadChange);
        }
    }
    public void reload() {
        registerAllFlammable();
    }

    // Helper to return the default flammability entries backed up during initialization
    public ArrayList<FlammableBlockEntry> getDefaultFlammabilityEntries() {
        return new ArrayList<>(defaultFlammability.values());
    }
}