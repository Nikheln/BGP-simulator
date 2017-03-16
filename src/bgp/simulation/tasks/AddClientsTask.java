package bgp.simulation.tasks;

import java.util.List;

import bgp.client.BGPClient;
import bgp.simulation.Simulator;

public class AddClientsTask extends SimulationTask {

	private final int amountOfClients;
	private final List<Integer> routers;
	
	public AddClientsTask(List<Integer> routers, int amountOfClients, long delay) {
		super(1, 0, delay);
		this.routers = routers;
		this.amountOfClients = amountOfClients;
	}

	@Override
	protected void runTask() throws Exception {
		(routers.isEmpty() ? Simulator.getReservedIds() : routers).stream()
		.map(id -> Simulator.getRouter(id))
		.forEach(router -> {
			for (int i = 0; i < amountOfClients; i++) {
				Simulator.registerClient(new BGPClient(router));
			}
		});
	}

	@Override
	public SimulationTaskType getType() {
		return SimulationTaskType.ADD_CLIENTS;
	}
}
