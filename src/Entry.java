import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class Entry {
    private int nextOffset;
    private int nameOffset;
    private int attributes;
    private int sharingAttributes;
    private int childOffset;
    private int dataSizeAllocated;
    private int dataSizeUsed;
    private int dataSizeUncompressed;
    private int createTime;
    private int accessTime;
    private int modifyTime;

    private final UTFHeader header;
    private final long entryOffset;
    private final String dictionary;
    private byte[] data;
    private boolean childrenLoaded = false;

    private final FileChannel fileChannel;
    private Entry parent = null;
    private List<Entry> children = new ArrayList<Entry>();

    public Entry(FileChannel ch, long offset, UTFHeader header_, String dictionary_) throws IOException {
        entryOffset = offset;
        header = header_;
        dictionary = dictionary_;
        fileChannel = ch;
        populateEntry();
    }

    private void populateEntry() throws IOException {
        fileChannel.position(entryOffset);
        ByteBuffer buf = ByteBuffer.allocate(11*4).order(ByteOrder.LITTLE_ENDIAN);
        fileChannel.read(buf);
        buf.flip();

        nextOffset = buf.getInt();
        nameOffset = buf.getInt();
        attributes = buf.getInt();
        sharingAttributes = buf.getInt();
        childOffset = buf.getInt();
        dataSizeAllocated = buf.getInt();
        dataSizeUsed = buf.getInt();
        dataSizeUncompressed = buf.getInt();
        createTime = buf.getInt();
        accessTime = buf.getInt();
        modifyTime = buf.getInt();
    }

    private void populateChildren() throws IOException {
        if (this.isFile() || childrenLoaded) {
            return;
        }
        childrenLoaded = true;
        Entry child = newEntry(fileChannel, header.treeOffset + childOffset);
        child.parent = this;
        children.add(child);
        while (child.nextOffset != 0) {
            child = newEntry(fileChannel, header.treeOffset + child.nextOffset);
            child.parent = this;
            children.add(child);
        }
    }

    private Entry newEntry(FileChannel ch, long offset) throws IOException {
        return new Entry(ch, offset, header, dictionary);
    }

    public void printData() {
        System.out.println("nextOffset: " + nextOffset + "\n" +
                "nameOffset: " + nameOffset + "\n" +
                "attributes: " + String.format("0x%02X", attributes) + "\n" +
                "sharingAttributes: " + sharingAttributes + "\n" +
                "childOffset: "+ childOffset + "\n" +
                "dataSizeAllocated: " + dataSizeAllocated + "\n" +
                "dataSizeUsed: " + dataSizeUsed + "\n" +
                "dataSizeUncompressed: " + dataSizeUncompressed + "\n" +
                "createTime: " + createTime + "\n" +
                "accessTime: " + accessTime + "\n" +
                "modifyTime: " + modifyTime);
    }

    public boolean isFile() {
        return (attributes & 0x000000FF) == 0x80;
    }

    public boolean isFolder() {
        return (attributes & 0x000000FF) == 0x10;
    }

    public boolean isRootNode() {
        return parent == null;
    }

    public void printTree() throws IOException {
        printTree(0);
    }

    private void printTree(int depth) throws IOException {
        String postfix = "";
        if (this.isFile()) {
            postfix = ": " + header.treeOffset + this.childOffset + ", " + this.dataSizeUsed;
        }
        System.out.println("-".repeat(depth * 2) + this.name() + postfix);
        for (Entry child : getChildren()) {
            child.printTree(depth + 1);
        }
    }

    public String name() {
        StringBuilder sb = new StringBuilder();
        int i = nameOffset;
        if (nameOffset > header.namesSizeUsed || nameOffset < 0) {
            return "Out of Bounds dictionary index: " + nameOffset;
        }
        while (i < dictionary.length() && dictionary.charAt(i) != '\0') {
            sb.append(dictionary.charAt(i));
            i++;
        }
        return sb.toString();
    }

    public List<Entry> getChildren() throws IOException {
        if (children.isEmpty() && isFolder()) {
            populateChildren();
        }
        return children;
    }

    public Entry getParent() {
        return parent;
    }

    public byte[] getData() throws IOException {
        if (data == null) {
            populateData();
        }
        return data;
    }

    @Override
    public String toString() {
        if (isFolder()) {
            return "[DIR] " + name();
        }
        if (isFile()) {
            return "[FILE] " + name();
        }
        return "[ENTRY] " + name();
    }

    private void populateData() throws IOException {
        if (this.isFolder()) {
            data = new byte[0];
            return;
        }
        fileChannel.position(header.dataOffset + childOffset);
        ByteBuffer buf = ByteBuffer.allocate(dataSizeUsed);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        fileChannel.read(buf);
        buf.flip();

        data = buf.array();
    }
}
