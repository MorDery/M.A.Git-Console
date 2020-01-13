import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;

public class Deleter {
    public static void deleteFile(String i_Path, File i_File) {
        Path rootPath = Paths.get(i_Path + "/" + i_File);
        try {
            Files.delete(rootPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteTextFileContent(Path textFilePath) throws IOException {
        Files.write(textFilePath, Collections.singleton(""));
    }

    public static void deleteDir(String i_Path) throws IOException {
        Path rootPath = Paths.get(i_Path);
        Files.walk(rootPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

}
