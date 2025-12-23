import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UTFReader {
    private static int  signature;
    private static int  version;
    private static int  treeOffset;
    private static int  treeSize;
    private static int  unusedEntryOffset;
    private static int  entrySize;
    private static int  namesOffset;
    private static int  namesSizeAllocated;
    private static int  namesSizeUsed;
    private static int  dataOffset;
    private static int  unusedOffset;
    private static int  unusedSize;
    private static long fileTime;

    public static void main(String[] args) throws IOException {
        String path = "/home/tim/freelancer/DATA/AUDIO/walker.utf";
        try (FileChannel ch = FileChannel.open(Paths.get(path), StandardOpenOption.READ)) {
            populateHeader(ch);
            verifyHeader();
            printHeader();
            readDictionary(ch);
        }
    }

    private static void populateHeader(FileChannel ch) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        signature = readInt(buf, ch);
        version = readInt(buf, ch);
        treeOffset = readInt(buf, ch);
        treeSize = readInt(buf, ch);
        unusedEntryOffset = readInt(buf, ch);
        entrySize = readInt(buf, ch);
        namesOffset = readInt(buf, ch);
        namesSizeAllocated = readInt(buf, ch);
        namesSizeUsed = readInt(buf, ch);
        dataOffset = readInt(buf, ch);
        unusedOffset = readInt(buf, ch);
        unusedSize = readInt(buf, ch);
        fileTime = readLong(buf, ch);
    }

    private static void getEntries(FileChannel ch) {

    }

    private static void printHeader() {
        System.out.println("signature:\t\t\t\t" + signature + "\n" +
                "version:\t\t\t\t" + version + "\n" +
                "treeOffset:\t\t\t\t" + treeOffset + "\n" +
                "treeSize:\t\t\t\t" + treeSize + "\n" +
                "unusedEntryOffset:\t\t" + unusedEntryOffset + "\n" +
                "entrySize:\t\t\t\t" + entrySize + "\n" +
                "namesOffset:\t\t\t" + namesOffset + "\n" +
                "namesSizeAllocated:\t\t" + namesSizeAllocated + "\n" +
                "namesSizeUsed:\t\t\t" + namesSizeUsed + "\n" +
                "dataOffset:\t\t\t\t" + dataOffset + "\n" +
                "unusedOffset:\t\t\t" + unusedOffset + "\n" +
                "unusedSize:\t\t\t\t" + unusedSize + "\n" +
                "fileTime:\t\t\t\t" + fileTime);
    }

    private static void verifyHeader() throws IOException {
        boolean a = signature == 0x20465455; // == "UTF "
        boolean b = version == 0x101;
        boolean c = entrySize == 44;
        boolean d = namesSizeUsed <= namesSizeAllocated;
        if (a && b && c && d) {
            return;
        }
        throw new IOException("Invalid header!");
    }

    private static void readDictionary(FileChannel ch) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(namesSizeAllocated);
        buf.limit(namesSizeUsed);
        ch.position(namesOffset);
        ch.read(buf);
        buf.flip();

        List<String> strings = new ArrayList<String>();
        strings.add("");
        int index = 0;
        for (int i = 0; i < namesSizeUsed-1; i++) {
            char a = (char) buf.get();
            if (a == '\0') {
                strings.add("");
                index++;
            } else {
                strings.set(index, strings.get(index) + a);
            }
        }

        for (String a : strings) {
            System.out.println(a);
        }
    }

    private static int readInt(ByteBuffer buf, FileChannel ch) throws IOException{
        buf.clear();
        buf.limit(4);
        ch.read(buf);
        buf.flip();

        return buf.getInt();
    }

    private static long readLong(ByteBuffer buf, FileChannel ch) throws IOException{
        buf.clear();
        buf.limit(8);
        ch.read(buf);
        buf.flip();

        return buf.getLong();
    }

}