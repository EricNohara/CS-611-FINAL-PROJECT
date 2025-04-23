import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class FileManager {
    // save a file to the upload directory
    public static boolean saveFile(File fileToUpload, String filePath) {
        Path path = Paths.get(filePath);
    
        try {
            if (!Files.exists(path)) Files.createDirectories(path);
    
            Path destination = path.resolve(fileToUpload.getName());
            Files.copy(fileToUpload.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
    
            System.out.println("File saved to: " + destination.toAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save file: " + e.getMessage());
            return false;
        }
    }

    // delete a file given a path (absolute or relative)
    public static boolean deleteFile(String filePath) {
        Path path = Paths.get(filePath);
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Error deleting file: " + e.getMessage());
            return false;
        }
    }
}