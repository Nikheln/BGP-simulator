package bgp.core.routing;

import java.util.HashSet;
import java.util.Set;

import bgp.core.Consts;

public class ASNode {
	
	protected final int asId;
	protected final int localPref;
	
	protected Set<ASNode> children;
	protected Set<ASNode> parents;
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
		this.children = new HashSet<>();
		this.parents = new HashSet<>();
		this.subnets = new HashSet<>();
	}
	
	public ASNode(int asId) {
		this(asId, Consts.DEFAULT_PREF);
	}
	
	@Override
	public int hashCode() {
		return asId;
	}
	
	protected void linkChild(ASNode newChild) {
		this.children.add(newChild);
		newChild.addParent(this);
	}
	
	protected void delete() {
		for (ASNode child : children) {
			child.deleteParent(this);
			child.resetDistAndPref();
		}
		// Recalculate values in a separate loop to avoid relying on
		// this node in case of inter-connected children
		for (ASNode child : children) {
			child.recalculateDistAndPref();
		}
		
		for (ASNode parent : parents) {
			parent.children.remove(this);
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
	
	private void addParent(ASNode parent) {
		if (!this.parents.contains(parent)) {
			this.parents.add(parent);
			this.recalculateDistAndPref();
		}
	}
	
	private void deleteParent(ASNode parent) {
		this.parents.remove(parent);
	}
	
	private void resetDistAndPref() {
		// Reset best distance for recalculation
		this.bestDistance = Consts.MAX_HOPS;
		// Reset best preference for recalculation
		this.bestPreference = Consts.DEFAULT_PREF;
	}
	
	private void recalculateDistAndPref() {
		boolean changes = false;
		for (ASNode parent : parents) {
			if (parent.bestDistance + 1 < this.bestDistance) {
				this.bestDistance = parent.bestDistance + 1;
				changes = true;
			}
			if (parent.bestPreference > this.bestPreference) {
				this.bestPreference = parent.bestPreference;
				changes = true;
			}
		}
		if (changes) {
			for (ASNode child : children) {
				child.resetDistAndPref();
			}
			for (ASNode child : children) {
				child.recalculateDistAndPref();
			}
		}
	}
	
	protected ASNode findBestNextHop(ASNode root) {
		// Stop recursion if looking at root (packets routed here)
		if (this == root) {
			return this;
		}
		
		ASNode bestSoFar = null;
		
		for (ASNode parent : parents) {
			// Stop recursion if at the second level of the graph
			if (parent == root) {
				return this;
			}
			if (bestSoFar == null
					|| parent.bestPreference > bestSoFar.bestPreference
					|| (parent.bestPreference == bestSoFar.bestPreference && parent.bestDistance < bestSoFar.bestDistance)) {
				bestSoFar = parent;
			}
		}
		return bestSoFar.findBestNextHop(root);
	}

}
