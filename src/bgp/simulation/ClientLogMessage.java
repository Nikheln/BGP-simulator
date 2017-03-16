package bgp.simulation;

import bgp.utils.Address;

public class ClientLogMessage extends LogMessage {
	
	private final Address address;

	public ClientLogMessage(Address clientAddress, String message, LogMessageType type) {
		super(-1, message, type);
		this.address = clientAddress;
	}
	

	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append((timestamp - Simulator.getSimulationStartTime())/1000.0);
		sb.append(" s] ");
		sb.append(address);	
		sb.append(": ");
		sb.append(message);
		sb.append("\n");
		
		return sb.toString();
	}

}
