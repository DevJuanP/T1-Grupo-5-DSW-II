import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.*;

// Uso: java ZipDocx.java <stageDir> <outDocx>
public class ZipDocx {
    public static void main(String[] args) throws Exception {
        Path stage = Paths.get(args[0]);
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(args[1]))))) {
            Files.walkFileTree(stage, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String name = stage.relativize(file).toString().replace(File.separatorChar, '/');
                    zos.putNextEntry(new ZipEntry(name));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        System.out.println("OK " + args[1] + " " + Files.size(Paths.get(args[1])) + " bytes");
    }
}
