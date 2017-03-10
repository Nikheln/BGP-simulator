package bgp.simulation.tasks;

import java.util.Optional;
import java.util.TimerTask;

public abstract class SimulationTask extends TimerTask {
	
	// Simulation tasks implementing this interface cause the UI to update
	public interface TopologyChanging { }
	
	public enum TaskState {
		WAITING,
		RUNNING,
		FINISHED,
		FAILED;
	}
	
	// Amount of times the task should be repeated, or 0 if indefinitely
	private final int repetitions;
	private int runCounter;
	
	private final long interval;
	private final long delay;
	
	private TaskState state;
	
	private Optional<Runnable> onFinish = Optional.empty();
	
	public SimulationTask(int repetitions, long interval, long delay) {
		this.repetitions = repetitions;
		this.runCounter = 0;
		this.interval = interval;
		this.delay = delay;
		
		this.state = TaskState.WAITING;
	}
	
	public long getInterval() {
		return interval;
	}
	
	public int getRepetitions() {
		return repetitions;
	}
	
	public long getDelay() {
		return delay;
	}
	
	public TaskState getState() {
		return state;
	}
	
	public void onFinish(Runnable r) {
		onFinish = Optional.ofNullable(r);
	}
	
	@Override
	public void run() {
		state = TaskState.RUNNING;
		
		try {
			runTask();	
		} catch (Exception e) {
			state = TaskState.FAILED;
			e.printStackTrace();
		}
		
		if (++runCounter >= repetitions) {
			this.cancel();

			if (state != TaskState.FAILED) {
				state = TaskState.FINISHED;
			}			
		} else {
			state = TaskState.WAITING;
		}
		
		onFinish.ifPresent(r -> r.run());
	}

	protected abstract void runTask() throws Exception;

}
