package uy.eleven.canasta.security.util;

import java.security.SecureRandom;

/**
 * Generates cryptographically secure API keys.
 * Format: sk_live_<64_hex_characters> (256-bit entropy)
 */
public class ApiKeyGenerator {

    private static final String PREFIX = "sk_live_";
    private static final int KEY_LENGTH_BYTES = 32; // 256 bits
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generate() {
        byte[] randomBytes = new byte[KEY_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return PREFIX + bytesToHex(randomBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(HEX_CHARS.charAt((b >> 4) & 0xf));
            hex.append(HEX_CHARS.charAt(b & 0xf));
        }
        return hex.toString();
    }
}
