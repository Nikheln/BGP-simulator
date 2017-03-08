package bgp.simulation.tasks;

import bgp.core.BGPRouter;
import bgp.core.messages.NotificationMessage;
import bgp.simulation.SimulatorState;

public class DisconnectRoutersTask extends SimulationTask {
	
	private final int breakingRouter, routerToDisconnect;

	public DisconnectRoutersTask(int breakingRouter, int routerToDisconnect, long delay) {
		super(1, 0, delay);
		
		this.breakingRouter = breakingRouter;
		this.routerToDisconnect = routerToDisconnect;
	}

	@Override
	protected void runTask() throws Exception {
		BGPRouter breaking = SimulatorState.getRouter(breakingRouter);
		
		if (breaking.hasConnectionTo(routerToDisconnect)) {
			breaking.getConnectionFor(routerToDisconnect)
				.raiseNotification(NotificationMessage.getCeaseError());
		} else {
			throw new IllegalArgumentException("Specified router not connected.");
		}
	}

}
