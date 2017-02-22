package bgp.core.routing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import bgp.core.messages.UpdateMessage;
import bgp.core.messages.pathattributes.AsPath;
import bgp.core.messages.pathattributes.NextHop;
import bgp.core.messages.pathattributes.Origin;
import bgp.core.messages.pathattributes.PathAttribute;
import bgp.core.network.Subnet;

public class RoutingEngine {
	
	private final int asId;
	
	private final SubnetNode subnetRootNode;
	
	/**
	 * Cache used to get previously used values. Cleared every time routing info changes.
	 */
	private final Map<Subnet, Integer> routingCache;
	
	private final Set<AsSequence> paths;
	
	public RoutingEngine(int asId) {
		this.asId = asId;
		this.subnetRootNode = new SubnetNode(null, Subnet.getSubnet(0, ~0));
		this.routingCache = new HashMap<>();
		this.paths = new HashSet<>();
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
				subnetNode.paths
				.stream()
				// Sort in case of multiple possible AS's
				.sorted((as1, as2) -> {
					if (as1.getLocalPref() != as2.getLocalPref()) {
						return as2.getLocalPref() - as1.getLocalPref();
					} else if (as1.getLength() != as2.getLength()) {
						return as1.getLength() - as2.getLength();
					} else {
						return as1.getFirstHop() - as2.getFirstHop();
					}
				})
				.findFirst()
				.map(sequence -> sequence.getFirstHop())
				.orElse(-1));
	}
	
	public void printRoutingTableInfo() {
		System.out.println("=== Subnets ===");
		Queue<SubnetNode> nodes = new LinkedList<>();
		nodes.add(subnetRootNode);
		while (!nodes.isEmpty()) {
			System.out.println(nodes.peek().subnet + ", amount of paths: " + nodes.peek().paths.size());
			nodes.addAll(nodes.poll().children);
		}
		System.out.println("=== ===");
	}
	
	public void addRoutingInfo(Subnet subnet, AsSequence asSeq) {
		
		SubnetNode parent = getBestMatchingSubnetNode(subnet);
		
		if (parent.subnet.equals(subnet)) {
			parent.addPath(asSeq);
		} else {
			SubnetNode newNode = new SubnetNode(parent, subnet);
			newNode.addPath(asSeq);
		}
		
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
		AsSequence matchingSequence = getMatchingSequence(hops);
		
		// Remove the revoked subnets from the originating AS
		
		
		// Add advertised connections based on the AS_PATH
		
		
		// Add subnets reachable in the originating node
		
	}
	
	private AsSequence getMatchingSequence(List<Integer> hops) {
		Optional<AsSequence> ms = paths.stream().filter(path -> {
			if (hops.size() != path.getLength()) {
				return false;
			}
			for (int i = 0; i < path.getLength(); i++) {
				if (hops.get(i) != path.getHop(i)) {
					return false;
				}
			}
			return true;
		}).findAny();
		
		if (ms.isPresent()) {
			return ms.get();
		} else {
			AsSequence s = new AsSequence(hops);
			return s;
		}
	}
}
