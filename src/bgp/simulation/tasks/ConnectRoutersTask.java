package bgp.simulation.tasks;

import bgp.core.BGPRouter;
import bgp.simulation.SimulatorState;

public class ConnectRoutersTask extends SimulationTask {
	
	private final int router1, router2;

	public ConnectRoutersTask(int router1, int router2, long delay) {
		super(1, 0, delay);
		this.router1 = router1;
		this.router2 = router2;
	}

	@Override
	public void runTask() throws Exception {
		BGPRouter r1 = SimulatorState.getRouter(router1);
		BGPRouter r2 = SimulatorState.getRouter(router2);
		
		BGPRouter.connectRouters(r1, r2);
	}

}
