package bgp.core.routing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import bgp.core.messages.UpdateMessage;
import bgp.core.messages.pathattributes.AsPath;
import bgp.core.messages.pathattributes.NextHop;
import bgp.core.messages.pathattributes.Origin;
import bgp.core.messages.pathattributes.PathAttribute;
import bgp.core.network.Address;
import bgp.core.network.Subnet;

public class RoutingEngine {
	
	private final SubnetNode subnetRootNode;
	
	private final ASNode asRootNode;
	
	private final Map<Integer, ASNode> asNodes;
	
	/**
	 * Cache used to get previously used values. Cleared every time routing info changes.
	 */
	private final Map<Subnet, Integer> routingCache;
	
	public RoutingEngine(int asId) {
		this.subnetRootNode = new SubnetNode(Subnet.getSubnet(0, ~0));
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
	
	public void addRoutingInfo(Subnet subnet, int asId) {
		ASNode asNode = asNodes.get(asId);
		SubnetNode parent = getBestMatchingSubnetNode(subnet);
		
		if (parent.subnet.equals(subnet)) {
			parent.linkContainingAS(asNode);
		} else {
			SubnetNode newNode = new SubnetNode(subnet);
			newNode.linkContainingAS(asNode);
			parent.linkChildNode(newNode);
		}
		
		routingCache.clear();
	}
	
	public void removeRoutingInfo(Subnet subnet, int asId) {
		ASNode asNode = asNodes.get(asId);
		SubnetNode bestMatch = getBestMatchingSubnetNode(subnet);
		
		if (bestMatch.subnet.equals(subnet)) {
			bestMatch.removeRouter(asNode);
			
			// If the AS contains no subnets, assume it is dropped from the network
			if (asNode.subnets.isEmpty()) {
				removeRouter(asId);
			}
			
			routingCache.clear();
		}
	}
	
	public void removeRouter(int asId) {
		if (this.asNodes.containsKey(asId)) {
			ASNode toRemove = this.asNodes.get(asId);
			toRemove.delete();
			this.asNodes.remove(asId);
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
		boolean hasChanged;
		
		do {
			hasChanged = false;
			for (SubnetNode n : current.children) {
				if (n.subnet.containsAddress(address)) {
					current = n;
					hasChanged = true;
					break;
				}
			}
		} while (hasChanged);
		return current;
	}

	/**
	 * Process an UPDATE message and update the routing information accordingly
	 * @param um
	 * @throws IllegalArgumentException
	 */
	public void handleUpdateMessage(UpdateMessage um) throws IllegalArgumentException {
		// Try to extract the mandatory Path attributes
		AsPath ap = null;
		NextHop nh = null;
		Origin o = null;
		for (PathAttribute b : um.getPathAttributes()) {
			if (b instanceof AsPath) {
				ap = (AsPath) b;
			} else if (b instanceof NextHop) {
				nh = (NextHop) b;
			} else if (b instanceof Origin) {
				o = (Origin) b;
			}
		}
		if (ap == null || nh == null || o == null) {
			throw new IllegalArgumentException("Missing mandatory Path attributes");
		}
		
		LinkedList<Integer> hops = ap.getIdSequence();
		int originatingAsId = hops.getLast();
		
		// Remove the revoked subnets from the originating AS
		for (Subnet s : um.getWithdrawnRoutes()) {
			removeRoutingInfo(s, originatingAsId);
		}
		
		// Add advertised connections based on the AS_PATH
		int previousNode = asRootNode.asId;
		for (Integer as : hops) {
			if (!asNodes.containsKey(as)) {
				asNodes.put(as, new ASNode(as));
			}
			addAsConnection(previousNode, as);
			previousNode = as;
		}
		
		// Add subnets reachable in the originating node
		for (Subnet s : um.getNLRI()) {
			addRoutingInfo(s, originatingAsId);
		}
	}
}
