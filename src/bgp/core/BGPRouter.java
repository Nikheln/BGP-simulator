package bgp.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bgp.core.messages.BGPMessage;
import bgp.core.messages.KeepaliveMessage;
import bgp.core.messages.NotificationMessage;
import bgp.core.messages.OpenMessage;
import bgp.core.messages.UpdateMessage;
import bgp.core.messages.UpdateMessageBuilder;
import bgp.core.messages.notificationexceptions.NotificationException;
import bgp.core.messages.notificationexceptions.UpdateMessageException;
import bgp.core.messages.pathattributes.AsPath;
import bgp.core.messages.pathattributes.NextHop;
import bgp.core.messages.pathattributes.Origin;
import bgp.core.messages.pathattributes.PathAttribute;
import bgp.core.network.Address;
import bgp.core.network.AddressProvider;
import bgp.core.network.InterASInterface;
import bgp.core.network.PacketEngine;
import bgp.core.network.Subnet;
import bgp.core.network.packet.PacketReceiver;
import bgp.core.network.packet.PacketRouter;
import bgp.core.routing.RoutingEngine;

public class BGPRouter implements PacketRouter, PacketReceiver, AddressProvider {
	
	public final int id;
	
	public final Subnet subnet;
	
	/**
	 * Map that pairs addresses to AS's
	 */
	private final Map<Long, Integer> addressToASId;
	/**
	 * Map that pairs AS id's to their corresponding connections
	 */
	private final Map<Integer, ASConnection> connections;
	private final Timer connectionKeepaliveTimer;
	
	private final ExecutorService packetProcessingThread;
	
	private final ExecutorService maintenanceThread;
	
	private final RoutingEngine routingEngine;
	
	/**
	 * Map that pairs Addresses to their corresponding IPv4 packet receivers.
	 */
	private final Map<Long, PacketReceiver> packetReceivers;
	
	private long addressingPointer;
	
	private long receivedPacketCount;
	
	public BGPRouter(int id, Subnet subnet) {
		this.id = id;
		
		this.addressToASId = new HashMap<>();
		this.connections = new HashMap<>();
		this.connectionKeepaliveTimer = new Timer(true);
		
		this.packetReceivers = new HashMap<>();
		this.subnet = subnet;
		this.addressingPointer = this.subnet.getAddress() + 1;
		
		this.packetProcessingThread = Executors.newSingleThreadExecutor();
		this.maintenanceThread = Executors.newSingleThreadExecutor();
		
		this.routingEngine = new RoutingEngine(this.id);
		// Register this router's subnet
		this.routingEngine.addRoutingInfo(this.subnet, this.id, 0, 200);
	}

	@Override
	public void routePacket(byte[] packet, ASConnection receivingConnection) {
		packetProcessingThread.execute(() -> {
			if (!PacketEngine.validatePacketHeader(packet)) {
				// Drop packet if checksum doesn't match
				return;
			}
			
			try {
				PacketEngine.decrementTTL(packet);
			} catch (IllegalArgumentException e) {
				// Drop packet if TTL == 0, otherwise decrement
				return;
			}
			
			long address = PacketEngine.extractRecipient(packet);
			// Decide the AS to forward to
			int nextHop = routingEngine.decidePath(address, true);
			
			if (nextHop == this.id) {
				// Packet is designated to this subnet
				PacketReceiver rec = packetReceivers.get(address);
				if (rec != null) {
					if (rec == this) {
						receivingConnection.raiseKeepaliveFlag();
					}
					rec.receivePacket(packet);
				}
			} else if (connections.containsKey(nextHop)
					&& !connections.get(nextHop).equals(receivingConnection)) {
				// Packet should be forwarded elsewhere
				// If preferred route is the router that sent the package,
				// drop it to avoid bouncing back and forth
				connections.get(nextHop).sendPacket(packet);
			} else {
				// No suitable next hop is found, drop packet
				return;
			}
		});
	}
	
	/**
	 * Forcefully send a packet via a designated interface, used to forward UPDATE messages when no routing info is available.
	 * @param packet
	 * @param nextHop
	 */
	private void sendViaInterface(byte[] packet, int nextHop) {
		packetProcessingThread.execute(() -> {
			if (connections.containsKey(nextHop)) {
				connections.get(nextHop).sendPacket(packet);
			}
		});
	}

	@Override
	public Address reserveAddress(PacketReceiver receiver) throws IllegalArgumentException {
		while (packetReceivers.containsKey(addressingPointer)) {
			addressingPointer = subnet.getAddress() + ((addressingPointer + 1) & ~subnet.getBitmask());
		}
		packetReceivers.put(addressingPointer, receiver);
		
		return Address.getAddress(addressingPointer);
	}

	@Override
	public void freeAddress(Address address) throws IllegalArgumentException {
		 if (!subnet.containsAddress(address)) {
			throw new IllegalArgumentException("Specified address is not in this subnet");
		} else if (!packetReceivers.containsKey(address.getAddress())) {
			throw new IllegalArgumentException("Specified address has not been registered");
		} else {
			packetReceivers.remove(address.getAddress());
		}
	}

	@Override
	public void receivePacket(byte[] pkg) {
		receivedPacketCount++;
		long senderAddress = PacketEngine.extractSender(pkg);
		int senderId = addressToASId.getOrDefault(senderAddress, -1);
		maintenanceThread.execute(() -> {
			try {
				byte[] body = PacketEngine.extractBody(pkg);
				BGPMessage m = BGPMessage.deserialize(body);
				if (m instanceof KeepaliveMessage && senderId != -1) {
					connections.get(senderId).raiseKeepaliveFlag();
				} else if (m instanceof NotificationMessage) {
					removeConnection(connections.get(senderId));
				} else if (m instanceof OpenMessage) {
					OpenMessage om = (OpenMessage) m;
					addressToASId.put(senderAddress, om.getASId());
					ASConnection conn = connections.get(om.getASId());
					if (conn != null) {
						conn.handleOpenMessage(om);
					}
				} else if (m instanceof UpdateMessage) {
					UpdateMessage um = (UpdateMessage)m;
					routingEngine.handleUpdateMessage(um);
					
					forwardUpdateMessage(um);
				}
				
			} catch (NotificationException e) {
				// Invalid BGP message received OR not a BGP message altogether
				if (connections.containsKey(senderId)) {
					connections.get(senderId).raiseNotification(
							e.buildNotification());	
				}
			}
		});
	}
	
	/**
	 * Send all routing info to a peer after a new connection has been established.
	 * To avoid all routes getting path length 1, prefixes of same length are sent
	 * in a message padded to correct length
	 * @param recipientAsId
	 */
	public void sendRoutingInformation(int recipientAsId) {
		ASConnection conn = connections.get(recipientAsId);
		Address ownAddress = conn.getAdapter().getOwnAddress();
		Address neighbourAddress = conn.getNeighbourAddress();
		UpdateMessage um = null;
		try {
			um = new UpdateMessageBuilder()
					.addPathAttribute(new AsPath(Arrays.asList(id)))
					.addPathAttribute(new NextHop(conn.getAdapter().getOwnAddress().getBytes()))
					.addPathAttribute(new Origin(1))
					.build();
		} catch (UpdateMessageException e) {
		}
		
		List<byte[]> ums = routingEngine.generateInitialUpdateMessages(um);
		
		for (byte[] msg : ums) {
			conn.sendPacket(PacketEngine.buildPacket(ownAddress, neighbourAddress, msg));
		}
	}
	
	public void forwardUpdateMessage(UpdateMessage um) {
		if (um.getWithdrawnRoutes().isEmpty() && um.getNLRI().isEmpty()) {
			// No information to forward
			return;
		}
		
		List<Integer> visitedIds = new ArrayList<>();
		for (PathAttribute p : um.getPathAttributes()) {
			if (p instanceof AsPath) {
				if (((AsPath)p).getIdSequence().contains(id)) {
					return;
				}
				visitedIds.addAll(((AsPath)p).getIdSequence());
				break;
			}
		}
		um.appendOwnId(id);
		connections.forEach((asId, connection) -> {
			if (!visitedIds.contains(asId)) {
				um.changeNextHop(connection.getAdapter().getOwnAddress().getBytes());
				byte[] umBytes = PacketEngine.buildPacket(connection.getAdapter().getOwnAddress(), connection.getNeighbourAddress(), um.serialize());
				sendViaInterface(umBytes, asId);
			}
		});
	}
	
	public void removeConnection(ASConnection toRemove) {
		int toRemoveId = getIdForConnection(toRemove);

		connections.remove(toRemoveId);	
		try {
			// Build an UPDATE message to inform neighbours
			UpdateMessageBuilder b = new UpdateMessageBuilder();
			
			b.addPathAttribute(new AsPath(new ArrayList<>()))
			 .addPathAttribute(new Origin(this.id))
			 .addPathAttribute(new NextHop(new byte[4]));

			for (Subnet s : routingEngine.getSubnetsBehind(toRemoveId)) {
				b.addWithdrawnRoutes(s);
			}
			UpdateMessage um = b.build();
			
			// Revoke all connections via the broken link
			routingEngine.handleUpdateMessage(um);

			// Send an UPDATE message to peers
			forwardUpdateMessage(um);
		} catch (UpdateMessageException e) {
			// UPDATE message processing failed
		}
	}
	
	private int getIdForConnection(ASConnection conn) {
		return connections.entrySet()
			.stream()
			.filter(entry -> entry.getValue().equals(conn))
			.map(Map.Entry::getKey)
			.findAny()
			.orElse(-1);
	}
	
	public ASConnection getConnectionFor(int otherId) {
		ASConnection conn = connections.computeIfAbsent(otherId, id -> new ASConnection(reserveAddress(this), this));
		// Register this receiver to interface's address
		packetReceivers.computeIfAbsent(conn.getAdapter().getOwnAddress().getAddress(), address -> this);
		return conn;
	}
	
	public boolean hasConnectionTo(int otherId) {
		return connections.containsKey(otherId);
	}
	
	public Collection<ASConnection> getAllConnections() {
		return connections.values();
	}
	
	public Set<Integer> getConnectedRouterIds() {
		return connections.keySet();
	}

	@Override
	public long getReceivedPacketCount() {
		return receivedPacketCount;
	}

	@Override
	public Address getAddress() {
		return null;
	}
	
	public RoutingEngine getRoutingEngine() {
		return routingEngine;
	}
	
	public void registerKeepaliveTask(TimerTask task, long delay, long period) {
		connectionKeepaliveTimer.scheduleAtFixedRate(task, delay, period);
	}
	
	@Override
	public void shutdown() {
		// Shut down all threads and timers
		packetProcessingThread.shutdownNow();
		maintenanceThread.shutdownNow();
		connectionKeepaliveTimer.cancel();
		
		// Inform clients
		packetReceivers.values()
			.stream()
			.filter(pr -> pr != this)
			.forEach(pr -> pr.shutdown());
		
		// Inform peers
		connections.values()
			.stream()
			.forEach(conn -> conn.raiseNotification(NotificationMessage.getCeaseError()));
	}
	
	

	
	/**
	 * Connect two BGPRouter to one another. Automatically starts the connection process.
	 * @param router1
	 * @param router2
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public static void connectRouters(BGPRouter router1, BGPRouter router2) throws IllegalArgumentException, IOException {
		if (router1.connections.containsKey(router2.id) || router2.connections.containsKey(router1.id)) {
			throw new IllegalArgumentException("Routers already connected");
		}
		ASConnection conn1 = router1.getConnectionFor(router2.id);
		InterASInterface adapter1 = conn1.getAdapter();
		ASConnection conn2 = router2.getConnectionFor(router1.id);
		InterASInterface adapter2 = conn2.getAdapter();
		
		// Connect the "cables"
		adapter1.connectNeighbourOutputStream(adapter2);
		adapter2.connectNeighbourOutputStream(adapter1);
		
		conn1.start(adapter2.getOwnAddress());
		conn2.start(adapter1.getOwnAddress());
	}

}
