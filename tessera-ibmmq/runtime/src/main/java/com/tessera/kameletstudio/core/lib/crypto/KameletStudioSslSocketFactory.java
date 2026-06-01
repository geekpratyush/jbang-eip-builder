package com.tessera.kameletstudio.core.lib.crypto;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Custom SSL Socket Factory for Kamelet Studio connectors in Quarkus.
 * Extends SSLSocketFactory to be directly usable in JMS connection factories.
 */
public class KameletStudioSslSocketFactory extends SSLSocketFactory {

    private String trustStorePath;
    private String trustStorePassword;
    private String keyStorePath;
    private String keyStorePassword;

    private SSLSocketFactory delegate;

    public void setTrustStorePath(String trustStorePath) { this.trustStorePath = trustStorePath; }
    public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
    public void setKeyStorePath(String keyStorePath) { this.keyStorePath = keyStorePath; }
    public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }

    private synchronized SSLSocketFactory getDelegate() {
        if (delegate == null) {
            try {
                delegate = createSocketFactory();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize SSLSocketFactory delegate", e);
            }
        }
        return delegate;
    }

    private KeyStore loadKeyStore(String path, String password) throws Exception {
        String type = "JKS";
        if (path != null && (path.endsWith(".p12") || path.endsWith(".pkcs12"))) {
            type = "PKCS12";
        }
        KeyStore ks = KeyStore.getInstance(type);
        try (FileInputStream fis = new FileInputStream(path)) {
            ks.load(fis, password != null ? password.toCharArray() : null);
        }
        return ks;
    }

    private SSLSocketFactory createSocketFactory() throws Exception {
        TrustManagerFactory tmf = null;
        if (trustStorePath != null && !trustStorePath.isEmpty()) {
            KeyStore ts = loadKeyStore(trustStorePath, trustStorePassword);
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
        }

        KeyManagerFactory kmf = null;
        if (keyStorePath != null && !keyStorePath.isEmpty()) {
            KeyStore ks = loadKeyStore(keyStorePath, keyStorePassword);
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

    @Override
    public String[] getDefaultCipherSuites() {
        return getDelegate().getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return getDelegate().getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return getDelegate().createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return getDelegate().createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
        return getDelegate().createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        return getDelegate().createSocket(address, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return getDelegate().createSocket(address, port, localAddress, localPort);
    }
}
