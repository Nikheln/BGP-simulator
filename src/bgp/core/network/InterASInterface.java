package bgp.core.network;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;

import bgp.core.Consts;
import bgp.core.SimulatorState;
import bgp.core.network.packet.PacketRouter;

public class InterASInterface implements AutoCloseable, Runnable {
	
	/**
	 * Maximum amount of bytes in the input buffer at any given time, 64 x MTU
	 */
	private static final int INPUT_BUFFER_LENGTH = Consts.MTU << 6;
	
	private Thread processingThread;
	
	private final Address ownAddress;
	
	private final PipedInputStream in;
	private final PipedOutputStream out;
	
	private final PacketRouter handler;
	
	private volatile boolean shutdown;
	
	public InterASInterface(Address ownAddress, PacketRouter handler) throws IllegalArgumentException {
		if (ownAddress == null) {
			throw new IllegalArgumentException("Address can not be null!");
		}
		if (!SimulatorState.isAddressFree(ownAddress)) {
			throw new IllegalStateException("Own address is already reserved");
		}
		
		SimulatorState.reserveAddress(ownAddress);
		this.ownAddress = ownAddress;
		
		this.in = new PipedInputStream(INPUT_BUFFER_LENGTH);
		this.out = new PipedOutputStream();
		
		this.handler = handler;
	}

	public void sendData(byte[] content) throws IOException {
		if (content != null && content.length > 0) {
			this.out.write(content.length);
			this.out.write(content, 0, content.length);
			this.out.flush();
		}
	}
	
	public void connectNeighbourOutputStream(InterASInterface other) throws IOException {
		this.in.connect(other.out);
		processingThread = new Thread(this);
		processingThread.start();
	}
	
	public Address getOwnAddress() {
		return ownAddress;
	}

	@Override
	public void run() {
		int octetCount = 0;
		byte[] readBuffer = new byte[Consts.MTU];
		while (!shutdown) {
			try {
				octetCount = in.read();
				in.read(readBuffer, 0, octetCount);
				handler.routePacket(Arrays.copyOf(readBuffer, octetCount));
			} catch (IOException e) {
				if (!shutdown) {
					// Actual error
					e.printStackTrace();
				} else {
					// Caused by shutdown
				}
			}
		}
	}

	@Override
	public void close() throws Exception {
		this.shutdown = true;
		
		SimulatorState.releaseAddress(ownAddress);
		
		Exception e = null;
		try {
			this.in.close();
		} catch (IOException e1) {
			e = e1;
		}
		try {
			this.out.close();
		} catch (IOException e1) {
			e = e1;
		}
		
		if (e != null) {
			throw e;
		}
	}

}
