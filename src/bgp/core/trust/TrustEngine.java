package bgp.core.trust;

import java.util.HashMap;
import java.util.Map;

public class TrustEngine implements TrustProvider {
	
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
	 * Modify the trust table based on the received message.
	 * Voted trust for a given neighbour is calculated as the average of given votes
	 * 
	 * @param tm
	 */
	public void handleTrustVote(int targetId, int votedTrust) {
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
	
	public void changeDirectTrust(int targetId, int delta) {
		byte oldTrust = directTrustValues.getOrDefault(targetId, (byte) 0);
		// Limit the new trust to range -128..127
		byte newTrust = (byte) Math.min(Math.max(oldTrust+delta, -128), 127);
		
		directTrustValues.put(targetId, newTrust);
		
	}

}

