package com.tessera.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public class CryptoStudioWindow {

    public static void show() {
        Stage stage = new Stage();
        stage.setTitle("Universal Crypto Studio");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("crypto-tab-pane");

        tabPane.getTabs().add(createAesTab(true));
        tabPane.getTabs().add(createAesTab(false));
        tabPane.getTabs().add(createBase64Tab(true));
        tabPane.getTabs().add(createBase64Tab(false));
        tabPane.getTabs().add(createUrlTab(true));
        tabPane.getTabs().add(createUrlTab(false));

        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10, 15, 0, 15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label("Crypto Studio");
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #eee;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnHelp = new Button("Help Guide", new FontIcon("fas-question-circle"));
        btnHelp.getStyleClass().add("toolbar-btn");
        btnHelp.setOnAction(e -> new RouteBuilderHelpWindow("Advanced Tools", "Crypto").show());
        topBar.getChildren().addAll(titleLbl, spacer, btnHelp);

        VBox layout = new VBox(10, topBar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        layout.getStyleClass().add("app-root");
        layout.getStyleClass().add(RouteBuilderApp.currentThemeClass);
        RouteBuilderApp.themedRoots.add(layout);
        stage.setOnHidden(e -> RouteBuilderApp.themedRoots.remove(layout));

        com.tessera.ui.components.ThemeManager.registerRoot(layout);
        Scene scene = new Scene(layout, 600, 550);
        try {
            scene.getStylesheets().add(CryptoStudioWindow.class.getResource("/styles/main.css").toExternalForm());
            if (RouteBuilderApp.currentDynamicCssUri != null) {
                scene.getStylesheets().add(RouteBuilderApp.currentDynamicCssUri);
            }
        } catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }

    private static Tab createAesTab(boolean encrypt) {
        Tab tab = new Tab(encrypt ? "AES Encrypt" : "AES Decrypt", new FontIcon(encrypt ? "fas-lock" : "fas-unlock"));
        tab.setClosable(false);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        Label title = new Label(encrypt ? "AES-256-GCM Encryption" : "AES-256-GCM Decryption");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label keyLbl = new Label("Secret Key / Password:");
        PasswordField keyField = new PasswordField();
        keyField.setPromptText("Enter encryption password...");
        String envKey = System.getenv("KAMELET_STUDIO_ENCRYPTION_KEY");
        if (envKey != null) keyField.setText(envKey);

        Label inputLbl = new Label(encrypt ? "Plaintext Input:" : "Base64 Ciphertext Input:");
        com.tessera.ui.components.MonacoEditorPane inputArea = new com.tessera.ui.components.MonacoEditorPane("text");
        inputArea.setPrefHeight(150);

        Button btnAction = new Button(encrypt ? "Encrypt" : "Decrypt", new FontIcon(encrypt ? "fas-shield-alt" : "fas-key"));
        btnAction.getStyleClass().add(encrypt ? "btn-validate" : "btn-decrypt");

        Label outputLbl = new Label(encrypt ? "Base64 Ciphertext Output:" : "Plaintext Output:");
        com.tessera.ui.components.MonacoEditorPane outputArea = new com.tessera.ui.components.MonacoEditorPane("text");
        outputArea.setPrefHeight(150);

        btnAction.setOnAction(e -> {
            String secret = keyField.getText();
            String input = inputArea.getText();
            if (secret.isEmpty() || input.isEmpty()) {
                outputArea.setText("Error: Key and Input cannot be empty.");
                return;
            }
            try {
                if (encrypt) {
                    outputArea.setText(aesEncrypt(input, secret));
                } else {
                    outputArea.setText(aesDecrypt(input.trim(), secret));
                }
            } catch (Exception ex) {
                outputArea.setText("Error: " + ex.getMessage());
            }
        });

        layout.getChildren().addAll(title, keyLbl, keyField, inputLbl, inputArea, btnAction, outputLbl, outputArea);
        tab.setContent(layout);
        return tab;
    }

    private static Tab createBase64Tab(boolean encode) {
        Tab tab = new Tab(encode ? "Base64 Encode" : "Base64 Decode", new FontIcon("fas-exchange-alt"));
        tab.setClosable(false);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        Label title = new Label(encode ? "Base64 Encoding" : "Base64 Decoding");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        com.tessera.ui.components.MonacoEditorPane inputArea = new com.tessera.ui.components.MonacoEditorPane("text");
        inputArea.setPrefHeight(150);

        Button btnAction = new Button(encode ? "Encode" : "Decode");
        btnAction.getStyleClass().add("toolbar-btn");

        com.tessera.ui.components.MonacoEditorPane outputArea = new com.tessera.ui.components.MonacoEditorPane("text");
        outputArea.setPrefHeight(150);

        btnAction.setOnAction(e -> {
            String input = inputArea.getText();
            if (input.isEmpty()) return;
            try {
                if (encode) {
                    outputArea.setText(Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8)));
                } else {
                    outputArea.setText(new String(Base64.getDecoder().decode(input.trim()), StandardCharsets.UTF_8));
                }
            } catch (Exception ex) {
                outputArea.setText("Error: " + ex.getMessage());
            }
        });

        layout.getChildren().addAll(title, inputArea, btnAction, outputArea);
        tab.setContent(layout);
        return tab;
    }

    private static Tab createUrlTab(boolean encode) {
        Tab tab = new Tab(encode ? "URL Encode" : "URL Decode", new FontIcon("fas-link"));
        tab.setClosable(false);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        Label title = new Label(encode ? "URL Encoding" : "URL Decoding");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        com.tessera.ui.components.MonacoEditorPane inputArea = new com.tessera.ui.components.MonacoEditorPane("text");
        inputArea.setPrefHeight(150);

        Button btnAction = new Button(encode ? "Encode" : "Decode");
        btnAction.getStyleClass().add("toolbar-btn");

        com.tessera.ui.components.MonacoEditorPane outputArea = new com.tessera.ui.components.MonacoEditorPane("text");
        outputArea.setPrefHeight(150);

        btnAction.setOnAction(e -> {
            String input = inputArea.getText();
            if (input.isEmpty()) return;
            try {
                if (encode) {
                    outputArea.setText(java.net.URLEncoder.encode(input, "UTF-8"));
                } else {
                    outputArea.setText(java.net.URLDecoder.decode(input, "UTF-8"));
                }
            } catch (Exception ex) {
                outputArea.setText("Error: " + ex.getMessage());
            }
        });

        layout.getChildren().addAll(title, inputArea, btnAction, outputArea);
        tab.setContent(layout);
        return tab;
    }

    private static String aesEncrypt(String plainText, String secret) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        
        byte[] combined = new byte[16 + 12 + cipherBytes.length];
        System.arraycopy(salt, 0, combined, 0, 16);
        System.arraycopy(iv, 0, combined, 16, 12);
        System.arraycopy(cipherBytes, 0, combined, 28, cipherBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    private static String aesDecrypt(String base64Text, String secret) throws Exception {
        byte[] decode = Base64.getDecoder().decode(base64Text);
        if (decode.length < 28) throw new IllegalArgumentException("Invalid ciphertext length");

        byte[] salt = new byte[16];
        System.arraycopy(decode, 0, salt, 0, 16);
        byte[] iv = new byte[12];
        System.arraycopy(decode, 16, iv, 0, 12);
        byte[] cipherBytes = new byte[decode.length - 28];
        System.arraycopy(decode, 28, cipherBytes, 0, cipherBytes.length);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    }
}
