package bgp.simulation.tasks;

import bgp.core.BGPRouter;
import bgp.core.messages.NotificationMessage;
import bgp.simulation.Simulator;
import bgp.simulation.tasks.SimulationTask.TopologyChanging;

public class DisconnectRoutersTask extends SimulationTask implements TopologyChanging {
	
	private final int breakingRouter, routerToDisconnect;

	public DisconnectRoutersTask(int breakingRouter, int routerToDisconnect, long delay) {
		super(1, 0, delay);
		
		this.breakingRouter = breakingRouter;
		this.routerToDisconnect = routerToDisconnect;
	}

	@Override
	protected void runTask() throws Exception {
		BGPRouter breaking = Simulator.getRouter(breakingRouter);
		
		if (breaking.hasConnectionTo(routerToDisconnect)) {
			breaking.getConnectionFor(routerToDisconnect, false)
				.ifPresent(c -> c.raiseNotification(NotificationMessage.getCeaseError()));
		} else {
			throw new IllegalArgumentException("Specified router not connected.");
		}
	}

	@Override
	public SimulationTaskType getType() {
		return SimulationTaskType.DISCONNECT_ROUTERS;
	}
}
