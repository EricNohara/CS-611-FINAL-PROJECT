import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Hasher {
    public static String hashPassword(String password) {
        try {
            byte[] salt = generateSalt(); // for higher security

            // configure SHA-256 with salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedBytes = md.digest(password.getBytes());

            // encode the hashed password and salt to store them together
            String hashedPassword = Base64.getEncoder().encodeToString(hashedBytes);
            String encodedSalt = Base64.getEncoder().encodeToString(salt);

            // combine hashed password and salt, so you can store both
            return encodedSalt + ":" + hashedPassword;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public static boolean checkPassword(String enteredPassword, String storedPassword) {
        try {
            // split the stored password into the salt and the hashed password
            String[] parts = storedPassword.split(":");
            String encodedSalt = parts[0];
            String storedHash = parts[1];

            // decode the salt and stored hash
            byte[] salt = Base64.getDecoder().decode(encodedSalt);
            byte[] hashedPassword = Base64.getDecoder().decode(storedHash);

            // hash entered password with salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] enteredHash = md.digest(enteredPassword.getBytes());

            // compare hashes
            return MessageDigest.isEqual(enteredHash, hashedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Error checking password", e);
        }
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16]; // 128-bit salt
        random.nextBytes(salt);
        return salt;
    }
}
