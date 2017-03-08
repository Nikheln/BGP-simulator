package bgp.simulation.tasks;

import bgp.core.BGPRouter;
import bgp.simulation.SimulatorState;
import bgp.utils.Subnet;

public class CreateRouterTask extends SimulationTask {

	private final int routerId;
	private final String subnet;
	
	public CreateRouterTask(long delay, int routerId, String subnet) {
		super(1, 0, delay);
		this.routerId = routerId;
		this.subnet = subnet;
	}

	@Override
	protected void runTask() throws Exception {
		BGPRouter r = new BGPRouter(routerId, Subnet.getSubnet(subnet));
		
		SimulatorState.registerRouter(r);
	}

}
