package bgp.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bgp.core.messages.BGPMessage;
import bgp.core.messages.KeepaliveMessage;
import bgp.core.messages.NotificationMessage;
import bgp.core.messages.OpenMessage;
import bgp.core.messages.UpdateMessage;
import bgp.core.network.Address;
import bgp.core.network.AddressProvider;
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
	
	private final RoutingEngine connectivityGraph;
	
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
		this.connectivityGraph = new RoutingEngine(this.id);
		// Register this router's subnet
		this.connectivityGraph.addRoutingInfo(this.subnet, this.id);
	}

	@Override
	public void routePacket(byte[] pkg) {
		packetProcessingThread.execute(() -> {
			if (!PacketEngine.verifyChecksum(pkg)) {
				// Drop packet if checksum doesn't match
				return;
			}
			
			try {
				PacketEngine.decrementTTL(pkg);
			} catch (IllegalArgumentException e) {
				// Drop packet if TTL == 0, otherwise decrement
				return;
			}
			
			long address = PacketEngine.extractRecipient(pkg);
			
			// Decide the AS to forward to
			int nextHop = connectivityGraph.decidePath(address);
			
			if (nextHop == this.id) {
				// Packet is designated to this subnet
				PacketReceiver rec = packetReceivers.get(address);
				if (rec != null) {
					rec.receivePacket(pkg);
				}
			} else if (connections.containsKey(nextHop)) {
				// Packet should be forwarded elsewhere
				connections.get(nextHop).sendPacket(pkg);
			} else {
				// No suitable next hop is found, drop packet
				return;
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
					
				} else if (m instanceof OpenMessage) {
					OpenMessage om = (OpenMessage) m;
					
				} else if (m instanceof UpdateMessage) {
					
				} else {
					
				}
				
			} catch (IllegalArgumentException e) {
				// Invalid BGP message received
			}
		});
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
		ASConnection conn2 = router2.getConnectionFor(router1.id);
		conn1.getAdapter().connectNeighbourOutputStream(conn2.getAdapter());
		conn2.getAdapter().connectNeighbourOutputStream(conn1.getAdapter());
		
		conn1.start();
		conn2.start();
	}
	
	private ASConnection getConnectionFor(int otherId) {
		return connections.computeIfAbsent(otherId, id -> new ASConnection(reserveAddress(this), this));
	}

	@Override
	public long getReceivedPacketCount() {
		return receivedPacketCount;
	}
	
	public void registerKeepaliveTask(TimerTask task, long period) {
		connectionKeepaliveTimer.scheduleAtFixedRate(task, period, period);
	}
	
	public void shutdown() {
		// Shut down all threads and timers
		packetProcessingThread.shutdownNow();
		maintenanceThread.shutdownNow();
		connectionKeepaliveTimer.cancel();
		
		// Inform clients
		
		// Inform peers
		
	}

}
