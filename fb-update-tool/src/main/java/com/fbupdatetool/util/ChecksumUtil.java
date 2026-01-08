package com.fbupdatetool.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumUtil {

    public static String calculateHash(Path path) {
        try {
            byte[] content = Files.readAllBytes(path);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(content);
            return bytesToHex(encodedhash);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao calcular hash do arquivo: " + path, e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}