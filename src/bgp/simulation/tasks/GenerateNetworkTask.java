package bgp.simulation.tasks;

import java.util.Queue;

import bgp.core.BGPRouter;
import bgp.simulation.LinkingOrder;
import bgp.simulation.Simulator;
import bgp.simulation.tasks.SimulationTask.TopologyChanging;
import bgp.utils.Subnet;

public class GenerateNetworkTask extends SimulationTask implements TopologyChanging {

	private final LinkingOrder topology;
	private final int networkSize;
	
	/**
	 * Automatically generates a network of a given topology and size at the beginning of the simulation.
	 * Should be followed by a long enough wait time to let the routers spread routing information.
	 * 
	 * @param topology
	 * @param networkSize
	 */
	public GenerateNetworkTask(LinkingOrder topology, int networkSize) {
		super(1, 0, 0);
		this.topology = topology;
		this.networkSize = networkSize;
	}

	@Override
	protected void runTask() throws Exception {
		Queue<Integer> ids = topology.getLinkingOrder(networkSize);
		for (int i = 1; i <= networkSize; i++) {
			Simulator.registerRouter(new BGPRouter(i, Subnet.getSubnet((100+i)+".0.0.0/8")));
		}
		
		while (!ids.isEmpty()) {
			int id1 = ids.poll();
			int id2 = ids.poll();
			
			if (id1 == id2) {
				continue;
			}
			
			BGPRouter r1 = Simulator.getRouter(id1);
			BGPRouter r2 = Simulator.getRouter(id2);
			
			if (r1.hasConnectionTo(id2) || r2.hasConnectionTo(id1)) {
				continue;
			}
			
			BGPRouter.connectRouters(r1, r2);
			
			// Sleep to avoid congesting the routers
			Thread.sleep(10);
		}
	}

	@Override
	public SimulationTaskType getType() {
		return SimulationTaskType.GENERATE_NETWORK;
	}

}
