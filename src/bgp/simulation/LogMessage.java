package bgp.simulation;

public class LogMessage {
	
	public enum LogMessageType {
		GENERAL("General"),
		KEEPALIVE("KEEPALIVE"),
		TRUST("TRUST"),
		ROUTING_INFO("Routing info"),
		CONNECTION("Connection");
		
		private final String uiText;
		
		private LogMessageType(String uiText) {
			this.uiText = uiText;
		}
		
		@Override
		public String toString() {
			return uiText;
		}
	}
	
	final long timestamp;
	final int originatingRouter;
	final String message;
	public final LogMessageType type;
	
	public LogMessage(int originatingRouter, String message, LogMessageType type) {
		this.timestamp = System.currentTimeMillis();
		this.originatingRouter = originatingRouter;
		this.message = message;
		this.type = type;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append((timestamp - SimulatorState.getSimulationStartTime())/1000.0);
		sb.append(" s] ");
		if (originatingRouter == 0) {
			sb.append("Global");
		} else {
			sb.append("Router ");
			sb.append(originatingRouter);	
		}
		sb.append(": ");
		sb.append(message);
		sb.append("\n");
		
		return sb.toString();
	}
}
