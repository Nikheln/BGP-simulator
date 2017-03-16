package bgp.simulation.tasks;

import bgp.core.BGPRouter;
import bgp.simulation.Simulator;
import bgp.simulation.tasks.SimulationTask.TopologyChanging;
import bgp.utils.Subnet;

public class CreateRouterTask extends SimulationTask implements TopologyChanging {

	private final int routerId;
	private final String subnet;
	
	public CreateRouterTask(int routerId, String subnet, long delay) {
		super(1, 0, delay);
		this.routerId = routerId;
		this.subnet = subnet;
	}

	@Override
	protected void runTask() throws Exception {
		BGPRouter r = new BGPRouter(routerId, Subnet.getSubnet(subnet));
		
		Simulator.registerRouter(r);
	}

	@Override
	public SimulationTaskType getType() {
		return SimulationTaskType.CREATE_ROUTER;
	}

}
