package bgp.simulation.tasks;

public class StopGeneratingTrafficTask extends SimulationTask {

	private final StartGeneratingTrafficTask taskToStop;
	
	public StopGeneratingTrafficTask(StartGeneratingTrafficTask taskToStop, long delay) {
		super(1, 0, delay);
		this.taskToStop = taskToStop;
	}

	@Override
	protected void runTask() throws Exception {
		this.taskToStop.getPingerClient().get().stopPinging();
	}

}
