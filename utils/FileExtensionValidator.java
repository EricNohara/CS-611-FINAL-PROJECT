package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileExtensionValidator {
    private static final Pattern VALID_EXTENSION_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    public static boolean isValid(String input) {
        if (input == null || input.trim().isEmpty()) return true; // allowed to have an empty string as input

        String[] types = input.split(",");
        Set<String> seen = new HashSet<>();

        for (String type : types) {
            String trimmed = type.trim();

            if (trimmed.isEmpty()) return false;

            // Optional: make validation case-insensitive
            String normalized = trimmed.toLowerCase();

            if (!VALID_EXTENSION_PATTERN.matcher(normalized).matches()) return false;

            if (!seen.add(normalized)) return false; // duplicate found
        }

        return true;
    }

    public static List<String> parseValidExtensions(String input) {
        if (input == null || input.trim().isEmpty()) return new ArrayList<>();

        return Arrays.stream(input.split(","))
                     .map(String::trim)
                     .map(String::toLowerCase)
                     .filter(VALID_EXTENSION_PATTERN.asPredicate())  // Filter only valid extensions
                     .collect(Collectors.toList());
    }
}

