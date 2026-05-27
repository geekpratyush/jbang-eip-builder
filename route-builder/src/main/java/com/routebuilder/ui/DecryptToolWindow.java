package com.routebuilder.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

public class DecryptToolWindow {

    public static void show() {
        Stage stage = new Stage();
        stage.setTitle("In-IDE Decrypt Tool (AES-256-GCM)");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getStyleClass().add("decrypt-window");
        layout.getStyleClass().add(RouteBuilderApp.currentThemeClass);
        RouteBuilderApp.themedRoots.add(layout);
        stage.setOnHidden(e -> RouteBuilderApp.themedRoots.remove(layout));

        Label titleLbl = new Label("AES-256-GCM Decryption Utility");
        titleLbl.getStyleClass().add("decrypt-title");

        Label keyLbl = new Label("Secret Key / Encryption Password:");
        keyLbl.getStyleClass().add("decrypt-label");
        
        PasswordField keyField = new PasswordField();
        keyField.getStyleClass().add("decrypt-field");
        keyField.setPromptText("Enter secret key or password...");
        
        // Pre-populate if system property KAMELET_STUDIO_ENCRYPTION_KEY exists
        String envKey = System.getenv("KAMELET_STUDIO_ENCRYPTION_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            keyField.setText(envKey);
        }

        Label cipherLbl = new Label("Base64 Encrypted Ciphertext:");
        cipherLbl.getStyleClass().add("decrypt-label");
        
        TextArea cipherArea = new TextArea();
        cipherArea.setPromptText("Paste your Base64 encrypted payload here...");
        cipherArea.setPrefHeight(100);
        cipherArea.setWrapText(true);
        cipherArea.getStyleClass().add("decrypt-area");

        Button btnDecrypt = new Button("Decrypt Payload");
        btnDecrypt.getStyleClass().add("decrypt-btn");
        btnDecrypt.setPrefWidth(150);

        Label plainLbl = new Label("Decrypted Plaintext Output:");
        plainLbl.getStyleClass().add("decrypt-label");
        
        TextArea plainArea = new TextArea();
        plainArea.setEditable(false);
        plainArea.setPrefHeight(150);
        plainArea.setWrapText(true);
        plainArea.getStyleClass().add("decrypt-output-area");

        btnDecrypt.setOnAction(e -> {
            String secret = keyField.getText();
            String base64Cipher = cipherArea.getText();
            if (secret.isEmpty() || base64Cipher.isEmpty()) {
                plainArea.setText("Error: Secret Key and Ciphertext cannot be empty.");
                return;
            }
            try {
                String decrypted = decrypt(base64Cipher.trim(), secret);
                plainArea.setText(decrypted);
            } catch (Exception ex) {
                plainArea.setText("Decryption Failed:\n" + ex.getMessage());
            }
        });

        layout.getChildren().addAll(
            titleLbl, 
            keyLbl, keyField, 
            cipherLbl, cipherArea, 
            btnDecrypt, 
            plainLbl, plainArea
        );

        Scene scene = new Scene(layout, 500, 520);
        try {
            scene.getStylesheets().add(DecryptToolWindow.class.getResource("/styles/main.css").toExternalForm());
            if (RouteBuilderApp.currentDynamicCssUri != null) {
                scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
            }
        } catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }

    private static String decrypt(String base64Text, String secret) throws Exception {
        byte[] decode = Base64.getDecoder().decode(base64Text);
        if (decode.length < 28) {
            throw new IllegalArgumentException("Ciphertext is too short. Must be at least 28 bytes (16 salt + 12 IV + payload).");
        }

        // Extract Salt, IV, and ciphertext
        byte[] salt = new byte[16];
        System.arraycopy(decode, 0, salt, 0, 16);

        byte[] iv = new byte[12];
        System.arraycopy(decode, 16, iv, 0, 12);

        byte[] cipherBytes = new byte[decode.length - 28];
        System.arraycopy(decode, 28, cipherBytes, 0, cipherBytes.length);

        // Key derivation matching PBKDF2WithHmacSHA256
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        // Decrypt GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        byte[] decryptedBytes = cipher.doFinal(cipherBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
