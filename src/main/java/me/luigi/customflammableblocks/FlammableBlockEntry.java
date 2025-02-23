package me.luigi.customflammableblocks;

public class FlammableBlockEntry {
    public String blockId;
    public int burnChance;
    public int spreadChance;

    public FlammableBlockEntry(String blockId, int burnChance, int spreadChance) {
        this.blockId = blockId;
        this.burnChance = burnChance;
        this.spreadChance = spreadChance;
    }
}