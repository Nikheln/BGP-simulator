package bgp.simulation.tasks;

import bgp.client.BGPClient;
import bgp.core.BGPRouter;
import bgp.simulation.SimulatorState;

public class AddClientsTask extends SimulationTask {

	private final int router, amountOfClients;
	
	public AddClientsTask(int router, int amountOfClients, long delay) {
		super(1, 0, delay);
		this.router = router;
		this.amountOfClients = amountOfClients;
	}

	@Override
	protected void runTask() throws Exception {
		BGPRouter r = SimulatorState.getRouter(router);
		
		for (int i = 0; i < amountOfClients; i++) {
			SimulatorState.registerClient(new BGPClient(r));
		}
	}

	@Override
	public SimulationTaskType getType() {
		return SimulationTaskType.ADD_CLIENTS;
	}
}
