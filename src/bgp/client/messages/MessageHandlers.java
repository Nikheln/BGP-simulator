package bgp.client.messages;

public class MessageHandlers {
	
	public interface Pingable {
		public void handlePing(long sender, PingRequest p);
	}
	
	public interface Pinger {
		/**
		 * Send a PING message to specified recipient
		 * @param recipient
		 * @return True if PING was sent, false otherwise
		 */
		public boolean sendPing(long recipient);
		public void receivePing(PingResponse p);
		public double getSuccessRate();
	}

}
