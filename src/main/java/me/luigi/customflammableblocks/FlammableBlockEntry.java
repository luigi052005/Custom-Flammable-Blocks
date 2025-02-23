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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlammableBlockEntry that = (FlammableBlockEntry) o;
        return blockId.equals(that.blockId);
    }
}