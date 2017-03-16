package bgp.simulation.tasks;

import bgp.core.BGPRouter;
import bgp.simulation.Simulator;

public class ChangeTrustTask extends SimulationTask {

	private final int changingRouter, changedRouter, delta;
	
	public ChangeTrustTask(int changingRouter, int changedRouter, int delta, int repetitions, long interval, long delay) {
		super(repetitions, interval, delay);
		this.changingRouter = changingRouter;
		this.changedRouter = changedRouter;
		this.delta = delta;
	}

	@Override
	protected void runTask() throws Exception {
		BGPRouter r = Simulator.getRouter(changingRouter);
		r.getTrustEngine().changeDirectTrust(changedRouter, delta);
	}

	@Override
	public SimulationTaskType getType() {
		return SimulationTaskType.CHANGE_TRUST;
	}
}
