package bgp.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.Test;

import bgp.core.messages.BGPMessage;
import bgp.core.messages.TrustMessage;
import bgp.core.messages.notificationexceptions.NotificationException;
import bgp.core.trust.TrustEngine;

public class TrustEngineTest {

	@Test
	public void testCrypto() {
		final String CRYPTO_PROVIDER = "SunMSCAPI";
		final String CRYPTO_ALGORITHM = "RSA";
		final int CRYPTO_KEYSIZE = 1024;
		
		final byte[] input = "testisana".getBytes();
		
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(CRYPTO_ALGORITHM, CRYPTO_PROVIDER);
			kpg.initialize(CRYPTO_KEYSIZE);
			KeyPair kp = kpg.generateKeyPair();
			PublicKey pub = kp.getPublic();
			byte[] pubEnc = pub.getEncoded();
			
			PrivateKey pri = kp.getPrivate();
			
			Cipher e = Cipher.getInstance(CRYPTO_ALGORITHM, CRYPTO_PROVIDER);
			e.init(Cipher.ENCRYPT_MODE, pub);
			byte[] b = e.doFinal(input);
			
			Cipher e2 = Cipher.getInstance(CRYPTO_ALGORITHM, CRYPTO_PROVIDER);
			PublicKey k2 = KeyFactory.getInstance(CRYPTO_ALGORITHM).generatePublic(new X509EncodedKeySpec(pubEnc));
			
			e2.init(Cipher.ENCRYPT_MODE, k2);
			byte[] b2 = e2.doFinal(input);
			
			
			Cipher d = Cipher.getInstance(CRYPTO_ALGORITHM, CRYPTO_PROVIDER);
			d.init(Cipher.DECRYPT_MODE, pri);
			byte[] result = d.doFinal(b);
			assertArrayEquals(input, result);
			result = d.doFinal(b2);
			assertArrayEquals(input, result);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidKeySpecException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSigning() throws Exception {
		TrustEngine t = new TrustEngine();
		TrustEngine t2 = new TrustEngine();
		
		byte[] payload = new byte[117];
		SecureRandom.getInstanceStrong().nextBytes(payload);
		
		byte[] cryptotext = TrustEngine.encryptData(t2.getPublicKey().getEncoded(), payload);
		byte[] signature = TrustEngine.signPayload(t.getPrivateKey().getEncoded(), cryptotext);
		
		byte[] cleartext = TrustEngine.decryptData(t2.getPrivateKey().getEncoded(), cryptotext);
		assertArrayEquals(payload, cleartext);
		assertTrue(TrustEngine.verifySignature(t.getPublicKey().getEncoded(), cryptotext, signature));
	}
	
	@Test
	public void testCryptoAndTrustMessages() throws Exception {
		final int REVIEWER_ID = 13;
		final int TARGET_ID = 10;
		final int TARGET_TRUST = 25;
		
		TrustEngine t = new TrustEngine();
		
		TrustMessage tm = new TrustMessage(REVIEWER_ID, TARGET_ID);
		byte[] serialized = tm.serialize();
		BGPMessage deserialized = null;
		try {
			deserialized = BGPMessage.deserialize(serialized);
		} catch (NotificationException e) {
			fail(e.getMessage());
		}
		if (!(deserialized instanceof TrustMessage)) {
			fail("Not a TRUST message");
		}
		TrustMessage tmD = (TrustMessage) deserialized;
		
		// Test correct deserialization
		assertEquals(tm.isRequest(), tmD.isRequest());
		assertEquals(tm.getTargetId(), tmD.getTargetId());
		
		TrustEngine t2 = new TrustEngine();
		t2.changeDirectTrust(TARGET_ID, TARGET_TRUST);
		
		// Generate response and signature to t
		byte[] encrTrust = null;
		byte[] signature = null;
		try {
			encrTrust = t2.getEncryptedTrust(tmD.getTargetId(), t.getPublicKey().getEncoded());
			signature = t2.getSignature(encrTrust);
			
			assertTrue(TrustEngine.verifySignature(t2.getPublicKey().getEncoded(), encrTrust, signature));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		TrustMessage resp = new TrustMessage(tmD.getReviewerId(), tmD.getTargetId(), encrTrust, signature);
		serialized = resp.serialize();
		try {
			deserialized = BGPMessage.deserialize(serialized);
		} catch (NotificationException e) {
			fail(e.toString());
		}
		tmD = (TrustMessage) deserialized;
		
		assertEquals(resp.isRequest(), tmD.isRequest());
		assertEquals(resp.getTargetId(), tmD.getTargetId());
		assertArrayEquals(resp.getPayload(), tmD.getPayload());
		assertArrayEquals(resp.getSignature(), tmD.getSignature());
		
		try {
			t.handleTrustVote(tmD.getTargetId(), t2.getPublicKey().getEncoded(), tmD.getPayload(), tmD.getSignature());
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
		// Make sure the trust was changed
		assertNotEquals(t.getTrustFor(TARGET_ID), 0);
	}

}
