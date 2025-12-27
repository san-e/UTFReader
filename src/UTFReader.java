import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class UTFReader {
    private UTFHeader header;

    private String dictionary = "";
    private FileChannel fileChannel;

    public void main(String[] args) throws IOException {
        String path = "/home/tim/freelancer/DATA/AUDIO/walker.utf";
        path = "/home/tim/freelancer/DATA/SHIPS/LIBERTY/LI_FIGHTER/li_fighter.cmp";
        try (FileChannel ch = FileChannel.open(Paths.get(path), StandardOpenOption.READ)) {
            fileChannel = ch;
            populateHeader();
            int entryCount = header.treeSize / header.entrySize;
            verifyHeader();
            printHeader();
            readDictionary();
            Entry root = new Entry(ch, header.treeOffset, header, dictionary);
            printTree(root, 0);
        }
    }

    private void printTree(Entry entry, int depth) {
        String postfix = "";
        if (entry.isFile()) {
            postfix = ": " + header.treeOffset + entry.childOffset + ", " + entry.dataSizeUsed;
        }
        System.out.println("-".repeat(depth * 2) + entry.name() + postfix);
        for (Entry child : entry.children) {
            printTree(child, depth + 1);
        }
    }


    private void populateHeader() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header = new UTFHeader(
                readInt(buf),
                readInt(buf),
                readInt(buf),
                readInt(buf),
                readInt(buf),
                readInt(buf),
                readInt(buf),
                readInt(buf),
                readInt(buf),
                readInt(buf),
                readInt(buf),
                readInt(buf),
                readLong(buf)
        );
    }



    private void printHeader() {
        System.out.println("signature:\t\t\t\t" + header.signature + "\n" +
                "version:\t\t\t\t" + header.version + "\n" +
                "treeOffset:\t\t\t\t" + header.treeOffset + "\n" +
                "treeSize:\t\t\t\t" + header.treeSize + "\n" +
                "unusedEntryOffset:\t\t" + header.unusedEntryOffset + "\n" +
                "entrySize:\t\t\t\t" + header.entrySize + "\n" +
                "namesOffset:\t\t\t" + header.namesOffset + "\n" +
                "namesSizeAllocated:\t\t" + header.namesSizeAllocated + "\n" +
                "namesSizeUsed:\t\t\t" + header.namesSizeUsed + "\n" +
                "dataOffset:\t\t\t\t" + header.dataOffset + "\n" +
                "unusedOffset:\t\t\t" + header.unusedOffset + "\n" +
                "unusedSize:\t\t\t\t" + header.unusedSize + "\n" +
                "fileTime:\t\t\t\t" + header.fileTime);
    }

    private void verifyHeader() throws IOException {
        boolean a = header.signature == 0x20465455; // == "UTF "
        boolean b = header.version == 0x101;
        boolean c = header.entrySize == 44;
        boolean d = header.namesSizeUsed <= header.namesSizeAllocated;
        if (a && b && c && d) {
            return;
        }
        throw new IOException("Invalid header!");
    }

    private void readDictionary() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(header.namesSizeUsed);
        fileChannel.position(header.namesOffset);
        fileChannel.read(buf);
        buf.flip();

        for (int i = 0; i < header.namesSizeUsed; i++) {
            dictionary += (char) buf.get();
        }
    }

    private int readInt(ByteBuffer buf) throws IOException{
        buf.clear();
        buf.limit(4);
        fileChannel.read(buf);
        buf.flip();

        return buf.getInt();
    }

    private long readLong(ByteBuffer buf) throws IOException{
        buf.clear();
        buf.limit(8);
        fileChannel.read(buf);
        buf.flip();

        return buf.getLong();
    }
}