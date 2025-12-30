import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class UTFReader {
    private UTFHeader header;
    private String dictionary = "";
    private FileChannel fileChannel;
    public Entry root;

    public static void main(String[] args) throws IOException { // in practice this should be removed, but im experimenting
        String path = "/home/tim/freelancer/DATA/AUDIO/walker.utf";
        path = "/home/tim/freelancer/DATA/SHIPS/LIBERTY/LI_FIGHTER/li_fighter.cmp";
        path = "/home/tim/freelancer/DATA/SHIPS/LIBERTY/li_playerships.mat";

        UTFReader utf = new UTFReader(path);
        System.out.println();
        utf.root.printTree();
        Entry ddsNode = utf.root.children.get(1).children.getFirst().children.getFirst();
        byte[] dds = ddsNode.data;
        Files.write(Paths.get("/home/tim/freelancer/DATA/SHIPS/LIBERTY/" + ddsNode.parent.name() + ".dds"), dds);
    }

    public UTFReader(String path) throws IOException {
        try (FileChannel ch = FileChannel.open(Paths.get(path), StandardOpenOption.READ)) {
            fileChannel = ch;
            populateHeader();
            verifyHeader();
            printHeader();
            populateDictionary();
            root = new Entry(ch, header.treeOffset, header, dictionary);
        }
    }

    private void populateHeader() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(12*4 + 8).order(ByteOrder.LITTLE_ENDIAN);
        fileChannel.read(buf);
        buf.flip();
        header = new UTFHeader(
                buf.getInt(),
                buf.getInt(),
                buf.getInt(),
                buf.getInt(),
                buf.getInt(),
                buf.getInt(),
                buf.getInt(),
                buf.getInt(),
                buf.getInt(),
                buf.getInt(),
                buf.getInt(),
                buf.getInt(),
                buf.getLong()
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
        boolean e = header.treeSize % header.entrySize == 0;
        if (a && b && c && d && e) {
            return;
        }
        throw new IOException("Invalid header!");
    }

    private void populateDictionary() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(header.namesSizeUsed);
        fileChannel.position(header.namesOffset);
        fileChannel.read(buf);
        buf.flip();

        byte[] bytes = new byte[header.namesSizeUsed];
        buf.get(bytes);
        dictionary = new String(bytes, StandardCharsets.US_ASCII);
    }
}