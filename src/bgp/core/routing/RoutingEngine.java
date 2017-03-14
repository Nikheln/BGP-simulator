package bgp.core.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import bgp.core.messages.NotificationMessage.UpdateMessageError;
import bgp.core.messages.UpdateMessage;
import bgp.core.messages.notificationexceptions.UpdateMessageException;
import bgp.core.messages.pathattributes.AsPath;
import bgp.core.messages.pathattributes.NextHop;
import bgp.core.messages.pathattributes.Origin;
import bgp.core.messages.pathattributes.PathAttribute;
import bgp.core.trust.TrustProvider;
import bgp.simulation.Logger;
import bgp.simulation.LogMessage.LogMessageType;
import bgp.utils.Consts;
import bgp.utils.Subnet;

public class RoutingEngine {
	
	private final int asId;
	
	private final SubnetNode subnetRootNode;
	
	private final Map<Integer, Integer> localPref;
	
	private final TrustProvider trustProvider;
	
	public RoutingEngine(int asId, TrustProvider trustProvider) {
		this.asId = asId;
		this.subnetRootNode = new SubnetNode(null, Subnet.getSubnet(0, ~0));
		// Packets with unknown subnet will go here (drop)
		this.subnetRootNode.setPath(-1, 999);
		this.localPref = new ConcurrentHashMap<>();
		
		this.trustProvider = trustProvider;
	}
	
	/**
	 * Decide the first hop for specified address
	 * @param address
	 * @return ID of the router to hop next to
	 */
	public int decidePath(long address) {
		return getBestMatchingSubnetNode(address).getFirstHop();
	}
	
	public void addRoutingInfo(Subnet subnet, int firstHop, int length, int localPref) {
		
		SubnetNode n = getBestMatchingSubnetNode(subnet);
		boolean newNode = !n.subnet.equals(subnet);
		// With a partial match, add a new node as a child
		if (newNode) {
			n = new SubnetNode(n, subnet);
		}
		
		if (newNode
				|| (localPref > getLocalPref(n.getFirstHop()))
				|| (localPref == getLocalPref(n.getFirstHop()) && length < n.getLength())) {
			n.setPath(firstHop, length);

			Logger.log("Learned new route to " + subnet + " via " + firstHop + ", length: "
					+ length, asId, LogMessageType.ROUTING_INFO);
		}
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
			for (Iterator<SubnetNode> iter = current.getChildIterator(); iter.hasNext();) {
				SubnetNode n = iter.next();
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
	 * @return Set of nodes the original sender of um should be made aware of
	 */
	public Set<SubnetNode> handleUpdateMessage(UpdateMessage um) throws UpdateMessageException {
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
		int firstHop = hops.size() > 0 ? hops.get(0) : -1;
		int localPref = getLocalPref(firstHop);
		
		// Remove the revoked subnets if their preferred path is the revoking one
		Set<Subnet> deletedPaths = new HashSet<>();
		Set<SubnetNode> replyPaths = new HashSet<>();
		for (Subnet s : um.getWithdrawnRoutes()) {
			SubnetNode n = getBestMatchingSubnetNode(s);
			if (n.subnet.equals(s)) {
				// Exact match was found
				if (firstHop == n.getFirstHop() || firstHop == -1) {
					n.delete();
					deletedPaths.add(n.subnet);

					Logger.log("Revoked route to " + s + " via " + firstHop, asId, LogMessageType.ROUTING_INFO);
				} else {
					// Revoking peer should be informed of alternative route
					replyPaths.add(n);
				}
			}
		}
		
		// Add subnets reachable in this path if they are
		// preferred to current path or current path does not exist
		Set<Subnet> utilizedPaths = new HashSet<>();
		for (Subnet s : um.getNLRI()) {
			SubnetNode n = getBestMatchingSubnetNode(s);
			boolean pathChanged = false;
			int currentPathLocalPref = getLocalPref(n.getFirstHop());
			
			// With a partial match, add a new node as a child
			boolean newNode = !n.subnet.equals(s);
			if (newNode) {
				// New path
				n = new SubnetNode(n, s);
				pathChanged = true;
				
			} else if (localPref > currentPathLocalPref) {
				// Higher preference than current path
				pathChanged = true;
				
			} else if (localPref == currentPathLocalPref) {
				// Same preference, compare lengths modified with trust
				double oldTrust = (trustProvider.getTrustFor(n.getFirstHop()) + 128)/255.0;
				double oldCost = n.getLength()*oldTrust;
				
				double newTrust = (trustProvider.getTrustFor(firstHop) + 128)/255.0;
				double newCost = length*newTrust;
				
				pathChanged = newCost < oldCost;
			}
			
			if (pathChanged) {
				n.setPath(firstHop, length);
				Logger.log("Learned new route to " + s + " via " + firstHop + ", length: "
						+ length, asId, LogMessageType.ROUTING_INFO);
				utilizedPaths.add(n.subnet);
			}
		}
		
		// Clear the NLRI and Withdrawn routes lists from the UPDATE message
		um.getWithdrawnRoutes().clear();
		um.getWithdrawnRoutes().addAll(deletedPaths);
		um.getNLRI().clear();
		um.getNLRI().addAll(utilizedPaths);
		
		return replyPaths;
	}
	
	private int getLocalPref(int asId) {
		return localPref.getOrDefault(asId, Consts.DEFAULT_PREF);
	}
	
	public void setLocalPref(int asId, int pref) {
		localPref.put(asId, pref);
	}
	
	/**
	 * Create UPDATE message with specified NLRI
	 * @param base UpdateMessage to add information to
	 * @param NLRIToSend
	 * @return List of serialized UPDATE messages
	 */
	public List<byte[]> generatePaddedUpdateMessages(UpdateMessage base, Set<SubnetNode> NLRIToSend) {
		Map<Integer, Set<SubnetNode>> nodes = new HashMap<>();
		for (Iterator<SubnetNode> iter = NLRIToSend.iterator(); iter.hasNext();) {
			SubnetNode n = iter.next();
			// Do not send own default route
			if (n.getFirstHop() > 0 && n.getLength() < 100) {
				nodes.computeIfAbsent(n.getLength(), len -> new HashSet<>()).add(n);
			}
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

	/**
	 * Create UPDATE messages sent as initial routing information after a new connection is created
	 * @param base {@link UpdateMessage} with PathAttributes set
	 * @return List of serialized UPDATE messages
	 */
	public List<byte[]> generatePaddedUpdateMessages(UpdateMessage base) {
		// Collect all nodes to a set
		Set<SubnetNode> nodes = new HashSet<>();
		for (Iterator<SubnetNode> iter = subnetRootNode.getSubnetNodeIterator(); iter.hasNext(); ) {
			nodes.add(iter.next());
		}
		
		return generatePaddedUpdateMessages(base, nodes);
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
