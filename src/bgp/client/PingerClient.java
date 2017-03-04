package bgp.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import bgp.client.messages.MessageHandlers.Pinger;
import bgp.client.messages.PingRequest;
import bgp.client.messages.PingResponse;
import bgp.core.BGPRouter;
import bgp.core.SimulatorState;
import bgp.utils.PacketEngine;

public class PingerClient extends BGPClient implements Pinger {
	
	private final Set<Token> tokens;
	private int pingLimit;
	private int pingsSent;
	private int responsesReceived;

	public PingerClient(BGPRouter router) {
		super(router);
		tokens = new HashSet<>();
		pingsSent = 0;
		responsesReceived = 0;
	}
	
	public void startPinging(List<Long> recipientAddresses, int pingLimit, int interval) {
		this.pingLimit = pingLimit*recipientAddresses.size();
		TimerTask task = new TimerTask() {
				
			@Override
			public void run() {
				for (long recipient : recipientAddresses) {
					boolean limitReached = sendPing(recipient);
					
					if (limitReached) {
						this.cancel();
						return;
					}
				}
				
			}
		};
		
		SimulatorState.addClientTask(task, interval);
	}
	
	@Override
	public boolean sendPing(long recipient) {
		if (pingsSent >= pingLimit) {
			return true;
		}
		
		PingRequest pr = new PingRequest();
		tokens.add(new Token(pr.getTokenBytes()));
		ph.routePacket(PacketEngine.buildPacket(address.getAddress(), recipient, pr.serialize()));
		pingsSent++;
		
		return false;
	}

	@Override
	public void receivePing(PingResponse p) {
		boolean success = tokens.remove(new Token(p.getTokenBytes()));
		if (success) {
			responsesReceived++;
		}
	}
	
	@Override
	public double getSuccessRate() {
		if (pingsSent != 0) {
			return 1.0*responsesReceived/pingsSent;
		} else {
			return 1.0;
		}
	}
	
	@Override
	public void receivePacket(byte[] pkg) {
		super.receivePacket(pkg);
	}
	
	private class Token {
		private final byte[] tokenBytes;
		
		public Token(byte[] token) {
			this.tokenBytes = token;
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(tokenBytes);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Token) {
				return Arrays.equals(((Token)obj).tokenBytes, this.tokenBytes);
			}
			return false;
		}
	}

}
