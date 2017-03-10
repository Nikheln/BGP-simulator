package bgp.simulation.tasks;

import bgp.simulation.SimulatorState;
import bgp.simulation.tasks.SimulationTask.TopologyChanging;

public class DeleteRouterTask extends SimulationTask implements TopologyChanging {
	
	private final int routerId;

	public DeleteRouterTask(int routerId, long delay) {
		super(1, 0, delay);
		this.routerId = routerId;
	}

	@Override
	protected void runTask() throws Exception {
		SimulatorState.getRouter(routerId).shutdown();
	}

}
