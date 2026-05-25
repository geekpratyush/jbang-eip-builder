package com.routebuilder.kameletstudio.core.lib.crypto;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Custom SSL Socket Factory for Kamelet Studio connectors.
 * Supports loading Keystores and Truststores from file paths.
 */
public class KameletStudioSslSocketFactory {

    private String trustStorePath;
    private String trustStorePassword;
    private String keyStorePath;
    private String keyStorePassword;

    public void setTrustStorePath(String trustStorePath) { this.trustStorePath = trustStorePath; }
    public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
    public void setKeyStorePath(String keyStorePath) { this.keyStorePath = keyStorePath; }
    public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }

    public SSLSocketFactory createSocketFactory() throws Exception {
        TrustManagerFactory tmf = null;
        if (trustStorePath != null && !trustStorePath.isEmpty()) {
            KeyStore ts = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                ts.load(fis, trustStorePassword != null ? trustStorePassword.toCharArray() : null);
            }
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
        }

        KeyManagerFactory kmf = null;
        if (keyStorePath != null && !keyStorePath.isEmpty()) {
            KeyStore ks = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                ks.load(fis, keyStorePassword != null ? keyStorePassword.toCharArray() : null);
            }
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword != null ? keyStorePassword.toCharArray() : null);
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
            kmf != null ? kmf.getKeyManagers() : null,
            tmf != null ? tmf.getTrustManagers() : null,
            new SecureRandom()
        );

        return sslContext.getSocketFactory();
    }
}
