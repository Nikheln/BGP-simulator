package bgp.core.routing;

import java.util.HashMap;
import java.util.Map;

import bgp.core.network.Subnet;

public class SubnetGraph {
	
	private final SubnetNode subnetRootNode;
	
	private final ASNode asRootNode;
	
	private final Map<Integer, ASNode> asNodes;
	
	/**
	 * Cache used to get previously used values. Cleared every time routing info changes.
	 */
	private final Map<Subnet, Integer> routingCache;
	
	public SubnetGraph(int asId) {
		this.subnetRootNode = new SubnetNode(null, Subnet.getSubnet(0, ~0));
		this.asRootNode = new ASNode(asId);
		this.asNodes = new HashMap<>();
		this.asNodes.put(asId, asRootNode);
		
		this.routingCache = new HashMap<>();
	}
	
	/**
	 * This is where the magic happens.
	 * 
	 * 1) Find the AS with longest matching prefix
	 * 2) Look for a local preference for paths to this AS
	 * 3) Find the shortest path to this AS
	 * 
	 * @param address
	 * @return Router ID of NEXT_HOP to be used
	 */
	public int decidePath(long address) {
		SubnetNode subnetNode = getBestMatchingSubnetNode(address);
		return routingCache.computeIfAbsent(subnetNode.subnet, uncachedSubnet -> 
				subnetNode.asSet
				.stream()
				// Sort in case of multiple possible AS's
				.sorted((as1, as2) -> {
					if (as1.bestPreference != as2.bestPreference) {
						return as2.bestPreference - as1.bestPreference;
					} else if (as1.bestDistance != as2.bestDistance) {
						return as1.bestDistance - as2.bestDistance;
					} else {
						return as1.asId - as2.asId;
					}
				})
				.findFirst()
				.map(node -> node.findBestNextHop(asRootNode))
				.map(bestNextNode -> bestNextNode.asId)
				.orElse(-1));
	}
	
	public void addRoutingInfo(Subnet subnet, int bgpId) {
		SubnetNode newNode = new SubnetNode(subnet);
		SubnetNode parent = getBestMatchingSubnetNode(subnet);
		
		parent.linkChildNode(newNode);
		
		routingCache.clear();
	}
	
	public void removeRoutingInfo(Subnet subnet, int bgpId) {
		SubnetNode current = getBestMatchingSubnetNode(subnet);
		ASNode currentAs = asNodes.get(bgpId);
		current.removeRouter(currentAs);
		
		routingCache.clear();
	}
	
	public void removeRouter(int bgpId) {
		if (this.asNodes.containsKey(bgpId)) {
			ASNode toRemove = this.asNodes.get(bgpId);
			toRemove.delete();
			this.asNodes.remove(bgpId);
			
			routingCache.clear();
		}
	}
	
	public void addAsConnection(int nearerAsId, int furtherAsId) throws IllegalArgumentException {
		if (!asNodes.containsKey(nearerAsId)) {
			throw new IllegalArgumentException("AS " + nearerAsId + " has not been registered.");
		}
		
		ASNode near = asNodes.get(nearerAsId);
		ASNode far = asNodes.computeIfAbsent(furtherAsId, newId -> new ASNode(newId));
		// Wherever you are...
		near.linkChild(far);
		
		routingCache.clear();
	}
	
	
	private SubnetNode getBestMatchingSubnetNode(Subnet subnet) {
		// current always points to a node that has address in its subnet
		SubnetNode current = subnetRootNode;
		// Tells if current has child nodes with a suitable subspace
		boolean hasSuitableChildren;
		
		do {
			hasSuitableChildren = false;
			for (SubnetNode n : current.children) {
				if (n.subnet.containsSubnet(subnet)) {
					current = n;
					hasSuitableChildren = true;
					break;
				}
			}
		} while (hasSuitableChildren);
		
		return current;
	}
	
	private SubnetNode getBestMatchingSubnetNode(long address) {
		SubnetNode current = subnetRootNode;
		boolean hasSuitableChildren;
		
		do {
			hasSuitableChildren = false;
			for (SubnetNode n : current.children) {
				if (n.subnet.containsAddress(address)) {
					current = n;
					hasSuitableChildren = true;
					break;
				}
			}
		} while (hasSuitableChildren);
		
		return current;
	}
}
