package bgp.simulation.tasks;

import java.util.List;

import bgp.client.BGPClient;
import bgp.core.BGPRouter;
import bgp.simulation.SimulatorState;

public class AddClientsTask extends SimulationTask {

	private final int amountOfClients;
	private final List<Integer> routers;
	
	public AddClientsTask(List<Integer> routers, int amountOfClients, long delay) {
		super(1, 0, delay);
		this.routers = routers;
		this.amountOfClients = amountOfClients;
	}
	
	public AddClientsTask(int amountOfClients, long delay) {
		this(SimulatorState.getReservedIds(), amountOfClients, delay);
	}

	@Override
	protected void runTask() throws Exception {
		for (int id : routers) {
			BGPRouter r = SimulatorState.getRouter(id);

			for (int i = 0; i < amountOfClients; i++) {
				SimulatorState.registerClient(new BGPClient(r));
			}
		}
	}

	@Override
	public SimulationTaskType getType() {
		return SimulationTaskType.ADD_CLIENTS;
	}
}
