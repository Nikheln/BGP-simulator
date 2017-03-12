package bgp.simulation.tasks;

import bgp.core.BGPRouter;
import bgp.simulation.SimulatorState;

public class ChangeLocalPrefTask extends SimulationTask {
	
	private final int changingRouter, targetRouter, newLocalPref;

	public ChangeLocalPrefTask(int changingRouter, int targetRouter, int newLocalPref, long delay) {
		super(1, 0, delay);
		this.changingRouter = changingRouter;
		this.targetRouter = targetRouter;
		this.newLocalPref = newLocalPref;
	}

	@Override
	protected void runTask() throws Exception {
		BGPRouter r = SimulatorState.getRouter(changingRouter);
		r.getRoutingEngine().setLocalPref(targetRouter, newLocalPref);
	}

	@Override
	public SimulationTaskType getType() {
		return SimulationTaskType.CHANGE_LOCAL_PREF;
	}
}
