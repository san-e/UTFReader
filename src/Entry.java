import javax.swing.*;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class Entry {
    public int nextOffset;
    public int nameOffset;
    public int attributes;
    public int sharingAttributes;
    public int childOffset;
    public int dataSizeAllocated;
    public int dataSizeUsed;
    public int dataSizeUncompressed;
    public int createTime;
    public int accessTime;
    public int modifyTime;

    private final UTFHeader header;
    public final long entryOffset;
    private final String dictionary;

    private FileChannel fileChannel;
    public List<Entry> children = new ArrayList<Entry>();

    public Entry(FileChannel ch, long offset, UTFHeader header_, String dictionary_) throws IOException {
        entryOffset = offset;
        header = header_;
        dictionary = dictionary_;
        fileChannel = ch;
        populateEntry();
        populateChildren();
    }

    private void populateEntry() throws IOException {
        fileChannel.position(entryOffset);
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        nextOffset = readInt(buf);
        nameOffset = readInt(buf);
        attributes = readInt(buf);
        sharingAttributes = readInt(buf);
        childOffset = readInt(buf);
        dataSizeAllocated = readInt(buf);
        dataSizeUsed = readInt(buf);
        dataSizeUncompressed = readInt(buf);
        createTime = readInt(buf);
        accessTime = readInt(buf);
        modifyTime = readInt(buf);
    }

    private void populateChildren() throws IOException {
        if (this.isFile()) {
            return;
        }
        Entry child = newEntry(fileChannel, header.treeOffset + childOffset);
        children.add(child);
        while (child.nextOffset != 0) {
            child = newEntry(fileChannel, header.treeOffset + child.nextOffset);
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

    public void printTree() {
        printTree(0);
    }

    private void printTree(int depth) {
        String postfix = "";
        if (this.isFile()) {
            postfix = ": " + header.treeOffset + this.childOffset + ", " + this.dataSizeUsed;
        }
        System.out.println("-".repeat(depth * 2) + this.name() + postfix);
        for (Entry child : this.children) {
            child.printTree(depth + 1);
        }
    }

    public String name() {
        StringBuilder sb = new StringBuilder();
        int i = nameOffset;
        while (dictionary.charAt(i) != '\0') {
            sb.append(dictionary.charAt(i));
            i++;
        }
        return sb.toString();
    }

    public byte[] getData() throws IOException {
        if (this.isFolder()) {
            return new byte[0];
        }
        fileChannel.position(header.dataOffset + childOffset);
        ByteBuffer buf = ByteBuffer.allocate(dataSizeUsed);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        fileChannel.read(buf);
        buf.flip();

        return buf.array();
    }

    private int readInt(ByteBuffer buf) throws IOException {
        buf.clear();
        int bytesRead = fileChannel.read(buf);
        if (bytesRead != 4) {
            throw new EOFException("Expected 4 bytes, got " + bytesRead);
        }
        buf.flip();
        return buf.getInt();
    }

    private long readLong(ByteBuffer buf) throws IOException {
        buf.clear();
        buf.limit(8);
        fileChannel.read(buf);
        buf.flip();

        return buf.getLong();
    }
}
