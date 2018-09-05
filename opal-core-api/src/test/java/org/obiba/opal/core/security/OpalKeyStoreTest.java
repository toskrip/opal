package org.obiba.opal.core.security;

import org.junit.Test;

import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class OpalKeyStoreTest {

  @Test
  public void defaultTrustManagerTest() throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init((KeyStore) null);
    System.out.println("JVM Default Trust Managers:");
    for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
      System.out.println(trustManager);

      if (trustManager instanceof X509TrustManager) {
        X509TrustManager x509TrustManager = (X509TrustManager) trustManager;
        System.out.println("\tAccepted issuers count : " + x509TrustManager.getAcceptedIssuers().length);
      }
    }
  }

  @Test
  public void defaultKeyStoreManagersTest() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
    KeyManagerFactory kmFact = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmFact.init(null, null);
    System.out.println("JVM Default Trust Managers:");
    for (KeyManager keyManager : kmFact.getKeyManagers()) {
      System.out.println(keyManager);
    }
  }

}
