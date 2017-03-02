package bgp.client.messages;

public class MessageHandlers {
	
	public interface Pingable {
		public void handlePing(long sender, PingRequest p);
	}
	
	public interface Pinger {
		public void sendPing(long recipient);
		public void receivePing(PingResponse p);
		public double getSuccessRate();
	}

}
