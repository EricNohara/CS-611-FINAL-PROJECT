package utils;

import java.io.IOException;
import java.nio.file.*;

public class FileSimilarityChecker {
    public static double getMaxSimilarity(String targetFilePathStr) throws IOException {
        if (targetFilePathStr == null || targetFilePathStr == "") return 0.0;

        Path targetFilePath = Paths.get(targetFilePathStr);
        Path directory = targetFilePath.getParent();
        String targetContent = new String(Files.readAllBytes(targetFilePath));

        String extension = getFileExtension(targetFilePath);
        SimilarityStrategy strategy = getStrategyForExtension(extension);

        if (strategy == null) return 0.0;

        double maxSimilarity = 0.0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file) && !file.equals(targetFilePath)) {
                    String otherContent = new String(Files.readAllBytes(file));
                    double similarity = strategy.computeSimilarity(targetContent, otherContent);
                    maxSimilarity = Math.max(maxSimilarity, similarity);
                }
            }
        }

        return maxSimilarity;
    }

    private static String getFileExtension(Path path) {
        String filename = path.getFileName().toString();
        int index = filename.lastIndexOf(".");
        return (index > 0) ? filename.substring(index + 1).toLowerCase() : "";
    }

    // selects the correct file diffing strategy based off of the type of file
    private static SimilarityStrategy getStrategyForExtension(String ext) {
        switch (ext) {
            case "txt":
                return new TfIdfCosineSimilarity();
            case "csv":
                return new TfIdfCosineSimilarity();
            case "md":
                return new TfIdfCosineSimilarity();
            case "json":
                return new TfIdfCosineSimilarity();
            case "xml":
                return new TfIdfCosineSimilarity();
            case "html":
                return new TfIdfCosineSimilarity();
            // case "png": return new ImageSimilarity(); // for future
            default:
                return null;
        }
    }
}
