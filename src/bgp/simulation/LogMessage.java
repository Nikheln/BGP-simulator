package bgp.simulation;

public class LogMessage {
	
	public enum LogMessageType {
		GENERAL,
		KEEPALIVE,
		TRUST,
		ROUTING_INFO,
		CONNECTION;
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
