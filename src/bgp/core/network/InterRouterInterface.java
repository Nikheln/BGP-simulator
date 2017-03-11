package bgp.core.network;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;

import bgp.core.messages.NotificationMessage;
import bgp.core.network.packet.PacketRouter;
import bgp.utils.Consts;

public class InterRouterInterface implements AutoCloseable, Runnable {
	
	/**
	 * Maximum amount of bytes in the input buffer at any given time, 64 x MTU
	 */
	private static final int INPUT_BUFFER_LENGTH = Consts.MTU << 6;
	
	private static final int SYNCHRONIZATION_BYTES = 10;
	
	private Thread processingThread;
	
	private final PipedInputStream in;
	private final PipedOutputStream out;
	
	private final PacketRouter handler;
	
	private final ASConnection conn;
	
	private volatile boolean shutdown;
	
	public InterRouterInterface(PacketRouter handler, ASConnection conn) throws IllegalArgumentException {
		this.in = new PipedInputStream(INPUT_BUFFER_LENGTH);
		this.out = new PipedOutputStream();
		
		this.handler = handler;
		this.conn = conn;
	}

	public synchronized void sendData(byte[] content) throws IOException {
		if (content != null && content.length > 0 && content.length < Consts.MTU) {
			
			// Send the amount of upcoming octets in two bytes
			this.out.write((content.length&0xFF00) >>> 8);
			
			this.out.write(content.length&0xFF);
			
			// Send the actual packet
			this.out.write(content, 0, content.length);
			this.out.flush();
		}
	}
	
	public void connectNeighbourOutputStream(InterRouterInterface other) throws IOException {
		this.in.connect(other.out);
		processingThread = new Thread(this);
		processingThread.start();
	}

	@Override
	public void run() {
		int octetCount = 0, in1 = 0, in2 = 0;
		byte[] readBuffer = new byte[Consts.MTU];
		while (!shutdown) {
			try {
				// Read two bytes to get the octet count of the packet
				in1 = in.read();
				in2 = in.read();
				octetCount = in1*256 + in2;
				
				in.read(readBuffer, 0, octetCount);
				
				handler.routePacket(Arrays.copyOf(readBuffer, octetCount), conn);
			} catch (IOException|IndexOutOfBoundsException e) {
				if (!shutdown) {
					// Actual error
					conn.raiseNotification(NotificationMessage.getCeaseError());
				} else {
					// Caused by shutdown
				}
			}
		}
	}

	@Override
	public void close() throws Exception {
		this.shutdown = true;
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
