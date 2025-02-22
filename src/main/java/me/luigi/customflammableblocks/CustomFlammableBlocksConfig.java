package me.luigi.customflammableblocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomFlammableBlocksConfig {
    public boolean enabled = true;
    public List<String> BURNABLE_BLOCKS = new ArrayList<>(Arrays.asList(
            "minecraft:stone",
            "minecraft:cobblestone",
            "minecraft:bricks"
    ));

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    public static CustomFlammableBlocksConfig load(Path configDir) {
        configPath = configDir.resolve("flammable_blocks.json");
        CustomFlammableBlocksConfig config = null;

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                config = GSON.fromJson(reader, CustomFlammableBlocksConfig.class);
            } catch (Exception e) {
                System.err.println("Failed to load config file: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (config == null) {
            config = new CustomFlammableBlocksConfig();
            System.err.println("Config file not found or invalid. Generating a default configuration.");
            config.save();
        }
        return config;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}