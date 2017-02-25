package bgp.core.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bgp.core.Consts;
import bgp.core.messages.NotificationMessage.UpdateMessageError;
import bgp.core.messages.notificationexceptions.UpdateMessageException;
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
	private final Map<Integer, Integer> localPref;
	
	public RoutingEngine(int asId) {
		this.asId = asId;
		this.subnetRootNode = new SubnetNode(null, Subnet.getSubnet(0, ~0));
		// Packets with unknown subnet will go here (drop)
		this.subnetRootNode.setPath(-1, 0, 999);
		this.routingCache = new HashMap<>();
		this.localPref = new HashMap<>();
	}
	
	/**
	 * Decide a path to given address, based on current routing information
	 * @param address
	 * @return Router ID of NEXT_HOP to be used
	 */
	public int decidePath(long address, boolean useCache) {
		SubnetNode subnetNode = getBestMatchingSubnetNode(address);
		if (useCache) {
			return routingCache.computeIfAbsent(subnetNode.subnet, uncachedSubnet -> subnetNode.getFirstHop());
		} else {
			return subnetNode.getFirstHop();
		}
	}
	
	/**
	 * Decide the path for address, use cache
	 * @param address
	 * @return
	 */
	public int decidePath(long address) {
		return decidePath(address, true);
	}
	
	public void addRoutingInfo(Subnet subnet, int firstHop, int length, int localPref) {
		
		SubnetNode n = getBestMatchingSubnetNode(subnet);
		boolean newNode = !n.subnet.equals(subnet);
		// With a partial match, add a new node as a child
		if (newNode) {
			n = new SubnetNode(n, subnet);
		}
		
		if (newNode
				|| (localPref > n.getLocalPref())
				|| (localPref == n.getLocalPref() && length < n.getLength())) {
			n.setPath(firstHop, localPref, length);
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
	
	public Set<Subnet> getSubnetsBehind(int asId) {
		Set<Subnet> results = new HashSet<>();
		
		for (Iterator<SubnetNode> iter = subnetRootNode.getSubnetNodeIterator(); iter.hasNext(); ) {
			SubnetNode next = iter.next();
			if (next.getFirstHop() == asId) {
				results.add(next.subnet);
			}
		}
		
		return results;
	}

	/**
	 * Process an UPDATE message and update the routing information accordingly.
	 * Modify the UPDATE message ready for forwarding by filtering out unnecessary routes.
	 * 
	 * @param um
	 * @throws UpdateMessageException
	 */
	public void handleUpdateMessage(UpdateMessage um) throws UpdateMessageException {
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
			throw new UpdateMessageException(UpdateMessageError.MISSING_WELL_KNOWN_ATTRIBUTE);
		} else if (o.getOriginValue() < 0 || o.getOriginValue() > 2) {
			throw new UpdateMessageException(UpdateMessageError.INVALID_ORIGIN_ATTRIBUTE);
		}
		
		LinkedList<Integer> hops = ap.getIdSequence();
		int length = hops.size();
		int firstHop = hops.get(0);
		int localPref = getLocalPref(firstHop);

		// Remove the revoked subnets if their preferred path is the revoking one
		Set<Subnet> deletedPaths = new HashSet<>();
		for (Subnet s : um.getWithdrawnRoutes()) {
			SubnetNode n = getBestMatchingSubnetNode(s);
			if (n.subnet.equals(s) && firstHop == n.getFirstHop()) {
				n.delete();
				deletedPaths.add(n.subnet);
			}
		}
		
		// Add subnets reachable in this path if they are
		// preferred to current path or current path does not exist
		Set<Subnet> utilizedPaths = new HashSet<>();
		for (Subnet s : um.getNLRI()) {
			SubnetNode n = getBestMatchingSubnetNode(s);
			boolean newNode = !n.subnet.equals(s);
			// With a partial match, add a new node as a child
			if (newNode) {
				n = new SubnetNode(n, s);
			}
			
			if (newNode
					|| (localPref > n.getLocalPref())
					|| (localPref == n.getLocalPref() && length < n.getLength())) {
				n.setPath(firstHop, localPref, length);
				utilizedPaths.add(n.subnet);
			}
		}
		
		// Clear the NLRI and Withdrawn routes lists from the UPDATE message
		um.getWithdrawnRoutes().clear();
		um.getWithdrawnRoutes().addAll(deletedPaths);
		um.getNLRI().clear();
		um.getNLRI().addAll(utilizedPaths);
		
		// Clear the routing cache to avoid issues with old information
		routingCache.clear();
	}
	
	private int getLocalPref(int asId) {
		return localPref.getOrDefault(asId, Consts.DEFAULT_PREF);
	}

	/**
	 * Create UPDATE messages sent as initial routing information after a new connection is created
	 * @param builder Builder with PathAttributes set
	 * @return List of serialized UpdateMessages to be sent to the peer
	 */
	public List<byte[]> generateInitialUpdateMessages(UpdateMessage base) {
		// Group the nodes based on path length
		Map<Integer, Set<SubnetNode>> nodes = new HashMap<>();
		for (Iterator<SubnetNode> iter = subnetRootNode.getSubnetNodeIterator(); iter.hasNext(); ) {
			SubnetNode n = iter.next();
			nodes.computeIfAbsent(n.getLength(), len -> new HashSet<>()).add(n);
		}
		
		List<byte[]> messages = new ArrayList<>();
		AsPath ap = extractAsPath(base);
		nodes.entrySet()
			.stream()
			.sorted((e1, e2) -> e2.getKey()-e1.getKey())
			.forEach(entry -> {
				// Padding to match real path length
				// Necessary to avoid the other end thinking of this as an optimal route to everything
				while (ap.getIdSequence().size() < entry.getKey()) {
					ap.appendId(asId);
				}
				base.getNLRI().clear();
				entry.getValue().stream().map(node -> node.subnet).forEach(subnet -> base.getNLRI().add(subnet));
				messages.add(base.serialize());
		});
		return messages;
	}
	
	private AsPath extractAsPath(UpdateMessage b) {
		for (PathAttribute pa : b.getPathAttributes()) {
			if (pa instanceof AsPath) {
				return (AsPath) pa;
			}
		}
		return null;
	}
}
