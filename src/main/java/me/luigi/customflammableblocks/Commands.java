package me.luigi.customflammableblocks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

public class Commands {
    public static final Logger LOG = LoggerFactory.getLogger("FlammableBlocksCommands");
    private final CustomFlammableBlocks mod;

    public Commands(CustomFlammableBlocks mod) {
        this.mod = mod;
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("flammable_blocks")
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            mod.reload();
                            context.getSource().sendMessage(Text.literal("Reloaded!"));
                            return 1;
                        }))

                .then(CommandManager.literal("list")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            // Build a list of block entries with burn and spread values
                            String blockList = mod.config.BURNABLE_BLOCKS.stream()
                                    .map(entry -> entry.blockId + " (burn: " + entry.burnChance + ", spread: " + entry.spreadChance + ")")
                                    .collect(java.util.stream.Collectors.joining(", "));
                            // Send the formatted list to the player
                            context.getSource().sendMessage(Text.literal("Burnable Blocks: " + blockList));
                            LOG.info("Current flammable blocks: {}", blockList);
                            return 1;
                        }))

                .then(CommandManager.literal("reset")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            // Reset config to Minecraft's default flammability entries
                            mod.config.BURNABLE_BLOCKS = new ArrayList<>(Arrays.asList(
                                    new FlammableBlockEntry("minecraft:cobblestone", 5, 20),
                                    new FlammableBlockEntry("minecraft:bricks", 5, 20)
                            ));
                            mod.BURNABLE_BLOCKS = mod.getDefaultFlammabilityEntries();
                            mod.config.save();
                            // Remove custom flammability and re-register the defaults
                            mod.registerAllFlammable();
                            context.getSource().sendMessage(Text.literal("Reset all flammable blocks to Minecraft's default values."));
                            return 1;
                        }))

                .then(CommandManager.literal("settings")
                        .then(CommandManager.literal("add")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    context.getSource().sendMessage(Text.literal("Usage: /flammable_blocks settings add <block id> <burn chance> <spread chance>"));
                                    return 1;
                                })
                                .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                Registries.BLOCK.getIds().stream().map(Identifier::toString),
                                                builder))
                                        .then(CommandManager.argument("burn", IntegerArgumentType.integer(0))
                                                .then(CommandManager.argument("spread", IntegerArgumentType.integer(0))
                                                        .executes(context -> {
                                                            Identifier identifier = IdentifierArgumentType.getIdentifier(context, "id");
                                                            String blockID = identifier.toString();
                                                            int burn = IntegerArgumentType.getInteger(context, "burn");
                                                            int spread = IntegerArgumentType.getInteger(context, "spread");
                                                            ServerCommandSource source = context.getSource();

                                                            // Validate block existence
                                                            if (!Registries.BLOCK.containsId(identifier)) {
                                                                source.sendMessage(Text.literal("Block not found: " + blockID));
                                                                return 0;
                                                            }

                                                            // Check if already added
                                                            boolean alreadyAdded = mod.config.BURNABLE_BLOCKS.stream()
                                                                    .anyMatch(entry -> entry.blockId.equals(blockID));
                                                            if (alreadyAdded) {
                                                                source.sendMessage(Text.literal("Block already in list!"));
                                                                return 0;
                                                            }

                                                            mod.config.BURNABLE_BLOCKS.add(new FlammableBlockEntry(blockID, burn, spread));
                                                            mod.config.save();

                                                            Block block = Registries.BLOCK.get(identifier);
                                                            mod.registerFlammable(block, burn, spread);
                                                            source.sendMessage(Text.literal("Added: " + blockID + " (burn: " + burn + ", spread: " + spread + ")"));
                                                            return 1;
                                                        })))))

                        .then(CommandManager.literal("add_all_non_flammable_blocks")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    int added = 0;

                                    for (Identifier id : Registries.BLOCK.getIds()) {
                                        Block block = Registries.BLOCK.get(id);

                                        // Skip air and fire
                                        if (block == Blocks.AIR || block == Blocks.FIRE) {
                                            continue;
                                        }

                                        // Check if the block is already flammable
                                        FlammableBlockRegistry.Entry flammability = FlammableBlockRegistry.getDefaultInstance().get(block);
                                        if (flammability != null && flammability.getBurnChance() > 0) {
                                            continue; // Skip if flammable
                                        }

                                        String blockID = id.toString();
                                        boolean alreadyAdded = mod.config.BURNABLE_BLOCKS.stream()
                                                .anyMatch(entry -> entry.blockId.equals(blockID));
                                        if (alreadyAdded) {
                                            continue;
                                        }

                                        mod.config.BURNABLE_BLOCKS.add(new FlammableBlockEntry(blockID, 5, 20));
                                        mod.BURNABLE_BLOCKS.add(new FlammableBlockEntry(blockID, 5, 20));
                                        mod.registerFlammable(block, 5, 20);
                                        added++;
                                    }

                                    mod.config.save();
                                    source.sendMessage(Text.literal("Added " + added + " new blocks to flammable list"));
                                    LOG.info("Added {} blocks to flammable list", added);
                                    return 1;
                                }))

                        .then(CommandManager.literal("remove")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    context.getSource().sendMessage(Text.literal("Please specify a block ID! EXAMPLE: minecraft:white_stained_glass"));
                                    return 1;
                                })
                                .then(CommandManager.argument("id", StringArgumentType.greedyString())
                                        // Suggest block IDs from the config entries
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                mod.config.BURNABLE_BLOCKS.stream().map(entry -> entry.blockId),
                                                builder))
                                        .executes(context -> {
                                            String blockID = StringArgumentType.getString(context, "id");
                                            Identifier identifier = Identifier.tryParse(blockID);
                                            ServerCommandSource source = context.getSource();

                                            if (identifier == null) {
                                                source.sendMessage(Text.literal("Invalid block ID format!"));
                                                return 0;
                                            }

                                            // Search for the matching entry in the config
                                            FlammableBlockEntry entryToRemove = null;
                                            for (FlammableBlockEntry entry : mod.config.BURNABLE_BLOCKS) {
                                                if (entry.blockId.equals(blockID)) {
                                                    entryToRemove = entry;
                                                    break;
                                                }
                                            }
                                            if (entryToRemove == null) {
                                                source.sendMessage(Text.literal("Block not in list: " + blockID));
                                                return 0;
                                            }
                                            mod.config.BURNABLE_BLOCKS.remove(entryToRemove);
                                            mod.BURNABLE_BLOCKS.remove(entryToRemove);
                                            mod.config.save();

                                            Block block = Registries.BLOCK.get(new Identifier(blockID));
                                            mod.registerFlammable(block, 0, 0);

                                            source.sendMessage(Text.literal("Removed: " + blockID));
                                            return 1;
                                        })))

                        .then(CommandManager.literal("enabled")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    context.getSource().sendMessage(Text.literal("Mod enabled: " + mod.enabled));
                                    return 1;
                                })
                                .then(CommandManager.argument("value", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean newEnabled = BoolArgumentType.getBool(context, "value");
                                            if (!newEnabled) {
                                                mod.BURNABLE_BLOCKS = mod.getDefaultFlammabilityEntries();
                                                mod.registerAllFlammable();
                                            } else {
                                                mod.registerAllFlammable();
                                            }
                                            mod.enabled = newEnabled;
                                            mod.config.enabled = newEnabled;
                                            mod.config.save();
                                            context.getSource().sendMessage(Text.literal("Mod enabled is set to: " + newEnabled));
                                            LOG.info("Mod enabled is set to {}", newEnabled);
                                            return 1;
                                        })))
                )
        );
    }
}