package bgp.core;

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
import bgp.core.network.PacketProcessor;
import bgp.core.network.Subnet;
import bgp.core.network.packet.PacketReceiver;
import bgp.core.network.packet.PacketRouter;
import bgp.core.routing.SubnetGraph;

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
	
	private final SubnetGraph connectivityGraph;
	
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
		this.connectivityGraph = new SubnetGraph(this.id);
		// Register this router's subnet
		this.connectivityGraph.addRoutingInfo(this.subnet, this.id);
	}

	@Override
	public void routePacket(byte[] pkg) {
		packetProcessingThread.execute(() -> {
			if (!PacketProcessor.verifyChecksum(pkg)) {
				// Drop packet if checksum doesn't match
				return;
			}
			
			try {
				PacketProcessor.decrementTTL(pkg);
			} catch (IllegalArgumentException e) {
				// Drop packet if TTL == 0, otherwise decrement
				return;
			}
			
			long address = PacketProcessor.extractRecipient(pkg);
			
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
		
		long senderAddress = PacketProcessor.extractSender(pkg);
		int senderId = addressToASId.getOrDefault(senderAddress, -1);
		maintenanceThread.execute(() -> {
			try {
				byte[] body = PacketProcessor.extractBody(pkg);
				BGPMessage m = BGPMessage.deserialize(body);
				
				
				if (m instanceof KeepaliveMessage && senderId != -1) {
					connections.get(senderId).raiseKeepaliveFlag();
				} else if (m instanceof NotificationMessage) {
					
				} else if (m instanceof OpenMessage) {
					
				} else if (m instanceof UpdateMessage) {
					
				} else {
					
				}
				
			} catch (IllegalArgumentException e) {
				// Invalid BGP message received
			}
		});
	}

	@Override
	public long getReceivedPacketCount() {
		return receivedPacketCount;
	}
	
	public void registerKeepaliveTask(TimerTask task, long period) {
		connectionKeepaliveTimer.scheduleAtFixedRate(task, 0, period);
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
