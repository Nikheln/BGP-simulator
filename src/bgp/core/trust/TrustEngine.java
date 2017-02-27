package bgp.core.trust;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

public class TrustEngine implements TrustProvider {

	private static final String CRYPTO_ALGORITHM = "RSA";
	private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
	private static final int CRYPTO_KEYSIZE = 1024;
	
	public static final int ENCRYPTED_MESSAGE_LENGTH = 128;
	public static final int ENCRYPTED_PAYLOAD_LENGTH = 117;
	
	// 1024-bit RSA keys used to transfer trust information
	private KeyPair kp;
	
	// The weight given to direct trust in range 0..1
	private final double directTrustWeight = 0.6;
	
	// Store the trust values for neighbours
	private final Map<Integer, Byte> votedTrustValues;
	private final Map<Integer, Byte> directTrustValues;
	// Count the amount of trust messages received for each neighbour
	private final Map<Integer, Integer> voteCounts;
	
	public TrustEngine() {
		this.votedTrustValues = new HashMap<>();
		this.directTrustValues = new HashMap<>();
		this.voteCounts = new HashMap<>();
		
		KeyPairGenerator kpg = null;
		try {
			kpg = KeyPairGenerator.getInstance(CRYPTO_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
		}
		kpg.initialize(CRYPTO_KEYSIZE);
		kp = kpg.generateKeyPair();
	}
	
	public PublicKey getPublicKey() {
		return kp.getPublic();
	}
	
	public PrivateKey getPrivateKey() {
		return kp.getPrivate();
	}
	
	@Override
	public byte getTrustFor(int targetId) {
		double votedTrust = votedTrustValues.computeIfAbsent(targetId, id -> 0);
		double directTrust = directTrustValues.getOrDefault(targetId, (byte) 0);
		byte totalTrust = (byte)(directTrustWeight * directTrust
				+ (1 - directTrustWeight) * votedTrust);
		
		return totalTrust;
	}
	
	/**
	 * Get the trust value for given target encrypted with a specified key.
	 * The value is padded with random numbers to hinder brute force attacks.
	 * 
	 * @param targetId
	 * @param encryptionKeyBytes
	 * @return 128-long byte array
	 * @throws Exception
	 */
	public byte[] getEncryptedTrust(int targetId, byte[] encryptionKeyBytes) throws Exception {
		byte[] payload = new byte[ENCRYPTED_PAYLOAD_LENGTH];
		// Fill with random numbers
		SecureRandom.getInstanceStrong().nextBytes(payload);
		// Set the actual trust to field 0
		payload[0] = getTrustFor(targetId);
		return encryptData(encryptionKeyBytes, payload);
	}
	
	public byte[] getSignature(byte[] payload) throws Exception {
		return signPayload(kp.getPrivate().getEncoded(), payload);
	}

	/**
	 * Get a signature for specified payload.
	 * @param payload
	 * @return
	 * @throws Exception
	 */
	public static byte[] signPayload(byte[] signingKey, byte[] payload) throws Exception {
		PrivateKey signing = KeyFactory.getInstance(CRYPTO_ALGORITHM)
				.generatePrivate(new PKCS8EncodedKeySpec(signingKey));
		Signature s = Signature.getInstance(SIGNATURE_ALGORITHM);
		s.initSign(signing);
		s.update(payload);
		
		return s.sign();
	}
	
	public static boolean verifySignature(byte[] verificationKey, byte[] payload, byte[] signature) throws Exception { 
		PublicKey verification = KeyFactory.getInstance(CRYPTO_ALGORITHM)
			.generatePublic(new X509EncodedKeySpec(verificationKey));
		Signature s = Signature.getInstance(SIGNATURE_ALGORITHM);
		s.initVerify(verification);
		s.update(payload);
		
		return s.verify(signature);
	}
	
	public static byte[] encryptData(byte[] encryptionKey, byte[] payload) throws Exception {
		PublicKey encryption = KeyFactory.getInstance(CRYPTO_ALGORITHM)
				.generatePublic(new X509EncodedKeySpec(encryptionKey));
		Cipher c = Cipher.getInstance(CRYPTO_ALGORITHM);
		c.init(Cipher.ENCRYPT_MODE, encryption);
		
		return c.doFinal(payload);
	}
	
	public static byte[] decryptData(byte[] decryptionKey, byte[] payload) throws Exception {
		PrivateKey decryption = KeyFactory.getInstance(CRYPTO_ALGORITHM)
				.generatePrivate(new PKCS8EncodedKeySpec(decryptionKey));
		Cipher c = Cipher.getInstance(CRYPTO_ALGORITHM);
		c.init(Cipher.DECRYPT_MODE, decryption);
		
		return c.doFinal(payload);
	}
	
	/**
	 * Decrypt a received trust vote with own private key.
	 * 
	 * @param input
	 * @return
	 * @throws Exception
	 */
	private byte decryptTrustVote(byte[] reviewerKey, byte[] input) throws Exception {
		PrivateKey decryptionKey = kp.getPrivate();
		Cipher c = Cipher.getInstance(CRYPTO_ALGORITHM);
		c.init(Cipher.DECRYPT_MODE, decryptionKey);
		
		// Actual vote is in the first octet by definition
		return c.doFinal(input)[0];
	}
	
	/**
	 * Modify the trust table based on the received message.
	 * Voted trust for a given neighbour is calculated as the average of given votes
	 * 
	 * @param tm
	 */
	public void handleTrustVote(int targetId, byte[] reviewerKey, byte[] encryptedVotedTrust, byte[] signature) throws Exception {
		if (!verifySignature(reviewerKey, encryptedVotedTrust, signature)) {
			throw new Exception("Signature does not match payload");
		}
		byte votedTrust = decryptTrustVote(reviewerKey, encryptedVotedTrust);
		byte oldTrust = votedTrustValues.getOrDefault(targetId, (byte) 0);
		// Increment the vote count
		voteCounts.put(targetId, voteCounts.getOrDefault(targetId, 0)+1);
		int voteCount = voteCounts.get(targetId);
		
		byte newTrust = (byte) (1.0*oldTrust + 1.0*(votedTrust - oldTrust)/voteCount);
		votedTrustValues.put(targetId, newTrust);
	}
	
	public void setDirectTrust(int targetId, byte trust) {
		directTrustValues.put(targetId, trust);
	}
	
	/**
	 * Change the direct trust of a specified peer
	 * @param targetId ID of the peer to be re-evaluated
	 * @param delta Amount of change, resulting value limited to range -128..127
	 */
	public void changeDirectTrust(int targetId, int delta) {
		byte oldTrust = directTrustValues.getOrDefault(targetId, (byte) 0);
		// Limit the new trust to range -128..127
		byte newTrust = (byte) Math.min(Math.max(oldTrust+delta, -128), 127);
		
		directTrustValues.put(targetId, newTrust);
		
	}

}


