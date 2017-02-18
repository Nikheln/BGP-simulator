package bgp.core.routing;

import java.util.HashSet;
import java.util.Set;

import bgp.core.Consts;

public class ASNode {
	
	protected final int asId;
	protected final int localPref;
	
	protected Set<ASNode> peers;
	protected Set<SubnetNode> subnets;
	
	/**
	 * Store the best possible distance to root
	 */
	protected int bestDistance = Consts.MAX_HOPS;
	/**
	 * Store the best available preference value on a path to root
	 */
	protected int bestPreference = Consts.DEFAULT_PREF;
	
	public ASNode(int asId, int localPref) {
		this.asId = asId;
		this.localPref = localPref;
		this.peers = new HashSet<>();
		this.subnets = new HashSet<>();
	}
	
	public ASNode(int asId) {
		this(asId, Consts.DEFAULT_PREF);
	}
	
	@Override
	public int hashCode() {
		return asId;
	}
	
	protected void delete() {
		for (ASNode other : peers) {
			this.peers.remove(other);
			other.peers.remove(this);
			other.resetDistAndPref();
		}
		
		for (ASNode other : peers) {
			// Recalculate in a separate loop to avoid issues caused by old values
			other.recalculateDistAndPref();
		}
		
		for (SubnetNode node : subnets) {
			node.removeRouter(this);
		}
	}
	
	protected void addContainedSubnet(SubnetNode node) {
		this.subnets.add(node);
	}
	
	protected void removeContainedSubnet(SubnetNode node) {
		this.subnets.remove(node);
	}
	
	private void resetDistAndPref() {
		// Reset best distance for recalculation
		this.bestDistance = Consts.MAX_HOPS;
		// Reset best preference for recalculation
		this.bestPreference = Consts.DEFAULT_PREF;
	}
	
	private void recalculateDistAndPref() {
		boolean changes = false;
		for (ASNode other : peers) {
			if (other.bestDistance + 1 < this.bestDistance) {
				this.bestDistance = other.bestDistance + 1;
				changes = true;
			}
			if (other.bestPreference > this.bestPreference) {
				this.bestPreference = other.bestPreference;
				changes = true;
			}
		}
		if (changes) {
			for (ASNode other : peers) {
				other.recalculateDistAndPref();
			}
		}
	}
	
	protected ASNode findBestNextHop(ASNode root) {
		// Stop recursion if looking at root (packets routed here)
		if (this == root) {
			return this;
		}
		
		ASNode bestSoFar = null;
		
		for (ASNode other : peers) {
			// Stop recursion if at the second level of the graph
			if (other == root) {
				return this;
			}
			if (bestSoFar == null
					|| other.bestPreference > bestSoFar.bestPreference
					|| (other.bestPreference == bestSoFar.bestPreference && other.bestDistance < bestSoFar.bestDistance)) {
				bestSoFar = other;
			}
		}
		System.out.println("Finding " + root.asId + " from " + this.asId + " via " + bestSoFar.asId + ", " + bestSoFar.bestDistance);
		return bestSoFar.findBestNextHop(root);
	}
	
	public static void linkNodes(ASNode n1, ASNode n2) {
		n1.peers.add(n2);
		n2.peers.add(n1);
		
		n1.recalculateDistAndPref();
		n2.recalculateDistAndPref();
	}

}
