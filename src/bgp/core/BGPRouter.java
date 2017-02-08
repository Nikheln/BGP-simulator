package bgp.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bgp.core.network.Address;
import bgp.core.network.AddressProvider;
import bgp.core.network.PacketProcessor;
import bgp.core.network.Subnet;
import bgp.core.network.packet.PacketReceiver;
import bgp.core.network.packet.PacketRouter;

public class BGPRouter implements PacketRouter, PacketReceiver, AddressProvider {
	
	public final int id;
	
	public final Subnet subnet;
	
	private final List<ASConnection> connections;
	
	private final ExecutorService packetProcessingThread;
	
	/**
	 * Map that pairs Addresses to their corresponding IPv4 packet receivers.
	 */
	private final Map<Long, PacketReceiver> packetReceivers;
	private long addressingPointer;
	
	private long receivedPacketCount;
	
	public BGPRouter(int id, Subnet subnet) {
		this.id = id;
		this.connections = new ArrayList<>();
		this.packetReceivers = new HashMap<>();
		this.subnet = subnet;
		this.addressingPointer = this.subnet.getAddress() + 1;
		
		this.packetProcessingThread = Executors.newSingleThreadExecutor();
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
			
			long address = pkg[16] << 24
					+ pkg[17] << 16
					+ pkg[18] << 8
					+ pkg[19];
			if (subnet.containsAddress(address)) {
				// Packet is designated to this subnet
				PacketReceiver rec = packetReceivers.get(address);
				if (rec != null) {
					rec.receivePacket(pkg);
				}
			} else {
				// Packet should be forwarded elsewhere
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
	}

	@Override
	public long getReceivedPacketCount() {
		return receivedPacketCount;
	}
	
	public void shutdown() {
		// Shut down packet routing
		packetProcessingThread.shutdownNow();
		
		// Inform clients
		
		// Inform peers
		
	}

}
