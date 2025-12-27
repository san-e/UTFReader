public final class UTFHeader {
    public final int signature;
    public final int version;
    public final int treeOffset;
    public final int treeSize;
    public final int unusedEntryOffset;
    public final int entrySize;
    public final int namesOffset;
    public final int namesSizeAllocated;
    public final int namesSizeUsed;
    public final int dataOffset;
    public final int unusedOffset;
    public final int unusedSize;
    public final long fileTime;

    public UTFHeader(
            int signature, int version, int treeOffset, int treeSize,
            int unusedEntryOffset, int entrySize, int namesOffset,
            int namesSizeAllocated, int namesSizeUsed, int dataOffset,
            int unusedOffset, int unusedSize, long fileTime) {

        this.signature = signature;
        this.version = version;
        this.treeOffset = treeOffset;
        this.treeSize = treeSize;
        this.unusedEntryOffset = unusedEntryOffset;
        this.entrySize = entrySize;
        this.namesOffset = namesOffset;
        this.namesSizeAllocated = namesSizeAllocated;
        this.namesSizeUsed = namesSizeUsed;
        this.dataOffset = dataOffset;
        this.unusedOffset = unusedOffset;
        this.unusedSize = unusedSize;
        this.fileTime = fileTime;
    }
}