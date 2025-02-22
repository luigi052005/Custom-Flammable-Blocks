package me.luigi.customflammableblocks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
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
                            // Convert the list to a readable string
                            String blockList = String.join(", ", mod.config.BURNABLE_BLOCKS);
                            // Send the formatted list to the player
                            context.getSource().sendMessage(Text.literal("Burnable Blocks: " + blockList));
                            LOG.info("Current flammable blocks: {}", blockList);
                            return 1;
                        }))

                .then(CommandManager.literal("reset")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            mod.config.BURNABLE_BLOCKS = new ArrayList<>(Arrays.asList(
                                    "minecraft:stone", // Default
                                    "minecraft:cobblestone",
                                    "minecraft:bricks"
                            ));
                            mod.config.save();
                            context.getSource().sendMessage(Text.literal("Config reset!"));
                            return 1;
                        }))

                .then(CommandManager.literal("settings")
                        .then(CommandManager.literal("add")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    context.getSource().sendMessage(Text.literal("Please specify a block ID! EXAMPLE: minecraft:white_stained_glass"));
                                    return 1;
                                })
                                .then(CommandManager.argument("id", StringArgumentType.greedyString())
                                        // suggest all registered blocks
                                        .suggests((context, builder) -> CommandSource.suggestMatching(Registries.BLOCK.getIds().stream().map(Identifier::toString), builder))
                                        .executes(context -> {
                                            String blockID = StringArgumentType.getString(context, "id");
                                            ServerCommandSource source = context.getSource();
                                            Identifier identifier = Identifier.tryParse(blockID);

                                            // Validate format
                                            if (identifier == null) {
                                                source.sendMessage(Text.literal("Invalid format! Use namespace:block_name"));
                                                return 0;
                                            }
                                            // Validate block existence
                                            if (!Registries.BLOCK.containsId(identifier)) {
                                                source.sendMessage(Text.literal("Block not found: " + blockID));
                                                return 0;
                                            }
                                            // Add to config
                                            if (mod.config.BURNABLE_BLOCKS.contains(blockID)) {
                                                source.sendMessage(Text.literal("Block already in list!"));
                                                return 0;
                                            }
                                            mod.config.BURNABLE_BLOCKS.add(blockID);
                                            mod.config.save();

                                            Block block = Registries.BLOCK.get(new Identifier(blockID));
                                            mod.registerFlammable(block);
                                            source.sendMessage(Text.literal("Added: " + blockID));
                                            return 1;
                                        })))

                        .then(CommandManager.literal("add_all_blocks")
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

                                        String blockId = id.toString();
                                        if (!mod.config.BURNABLE_BLOCKS.contains(blockId)) {
                                            mod.config.BURNABLE_BLOCKS.add(blockId);
                                            mod.registerFlammable(block);
                                            added++;
                                        }
                                    }

                                    mod.config.save();
                                    source.sendMessage(Text.literal("Added " + added + " new blocks to flammable list (excluding air)"));
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
                                        .suggests((context, builder) -> CommandSource.suggestMatching(mod.config.BURNABLE_BLOCKS, builder))
                                        .executes(context -> {
                                            String blockID = StringArgumentType.getString(context, "id");
                                            Identifier identifier = Identifier.tryParse(blockID);
                                            ServerCommandSource source = context.getSource();

                                            if (identifier == null) {
                                                source.sendMessage(Text.literal("Invalid block ID format!"));
                                                return 0;
                                            }
                                            // Remove regardless of existence, but warn if invalid
                                            if (!mod.config.BURNABLE_BLOCKS.contains(blockID)) {
                                                source.sendMessage(Text.literal("Block not in list: " + blockID));
                                                return 0;
                                            }
                                            mod.config.BURNABLE_BLOCKS.remove(blockID);
                                            mod.config.save();

                                            Block block = Registries.BLOCK.get(new Identifier(blockID));
                                            mod.removeFlammable(block);

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
                                                mod.resetAllFlammable();
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