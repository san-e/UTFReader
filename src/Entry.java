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

    public List<Entry> children = new ArrayList<Entry>();

    public Entry(FileChannel ch, long offset, UTFHeader header_, String dictionary_) throws IOException {
        entryOffset = offset;
        header = header_;
        dictionary = dictionary_;
        populateEntry(ch);
        populateChildren(ch);
    }

    private void populateEntry(FileChannel ch) throws IOException {
        ch.position(entryOffset);
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        nextOffset = readInt(buf, ch);
        nameOffset = readInt(buf, ch);
        attributes = readInt(buf, ch);
        sharingAttributes = readInt(buf, ch);
        childOffset = readInt(buf, ch);
        dataSizeAllocated = readInt(buf, ch);
        dataSizeUsed = readInt(buf, ch);
        dataSizeUncompressed = readInt(buf, ch);
        createTime = readInt(buf, ch);
        accessTime = readInt(buf, ch);
        modifyTime = readInt(buf, ch);
    }

    private void populateChildren(FileChannel ch) throws IOException {
        if (this.isFile()) {
            return;
        }
        Entry child = newEntry(ch, header.treeOffset + childOffset);
        children.add(child);
        while (child.nextOffset != 0) {
            child = newEntry(ch, header.treeOffset + child.nextOffset);
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

    public String name() {
        StringBuilder sb = new StringBuilder();
        int i = nameOffset;
        while (dictionary.charAt(i) != '\0') {
            sb.append(dictionary.charAt(i));
            i++;
        }
        return sb.toString();
    }

    private static int readInt(ByteBuffer buf, FileChannel ch) throws IOException {
        buf.clear();
        int bytesRead = ch.read(buf);
        if (bytesRead != 4) {
            throw new EOFException("Expected 4 bytes, got " + bytesRead);
        }
        buf.flip();
        return buf.getInt();
    }

    private static long readLong(ByteBuffer buf, FileChannel ch) throws IOException {
        buf.clear();
        buf.limit(8);
        ch.read(buf);
        buf.flip();

        return buf.getLong();
    }
}
