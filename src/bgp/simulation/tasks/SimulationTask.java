package bgp.simulation.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimerTask;

public abstract class SimulationTask extends TimerTask {
	
	public enum SimulationTaskType {
		NONE(""),
		GENERATE_NETWORK("Auto-generate network"),
		ADD_CLIENTS("Add clients"),
		CHANGE_LOCAL_PREF("Change local preference"),
		CHANGE_TRUST("Change direct trust"),
		CONNECT_ROUTERS("Connect routers"),
		CREATE_ROUTER("Create router"),
		DELETE_ROUTER("Delete router"),
		DISCONNECT_ROUTERS("Disconnect two routers"),
		START_GENERATING_TRAFFIC("Start generating traffic"),
		STOP_GENERATING_TRAFFIC("Stop generating traffic");
		
		public final String uiText;
		
		private SimulationTaskType(String uiText) {
			this.uiText = uiText;
		}
		
		@Override
		public String toString() {
			return uiText;
		}
		
	}
	
	// Simulation tasks implementing this interface cause the UI to update
	public interface TopologyChanging { }
	
	public enum TaskState {
		WAITING("Waiting..."),
		RUNNING("Running"),
		FINISHED("Finished"),
		FAILED("Failed"),
		CANCELED("Canceled");
		
		private String uiText;
		
		private TaskState(String uiText) {
			this.uiText = uiText;
		}
		
		@Override
		public String toString() {
			return uiText;
		}
	}
	
	// Amount of times the task should be repeated, or 0 if indefinitely
	private final int repetitions;
	private int runCounter;
	
	private final long interval;
	private final long delay;
	
	private TaskState state;
	
	private Optional<Runnable> onFinish = Optional.empty();
	private List<Runnable> stateChangeListeners = new ArrayList<>();
	
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
	
	private void setState(TaskState newState) {
		this.state = newState;
		stateChangeListeners.forEach(Runnable::run);
	}
	
	public void cancelTask() {
		this.state = TaskState.CANCELED;
		this.cancel();
	}
	
	public void addStateChangeListener(Runnable r) {
		stateChangeListeners.add(r);
	}
	
	@Override
	public void run() {
		if (state == TaskState.CANCELED) {
			return;
		}
		setState(TaskState.RUNNING);
		
		try {
			runTask();
		} catch (Exception e) {
			setState(TaskState.FAILED);
		}
		
		if (++runCounter >= repetitions) {
			this.cancel();

			if (state != TaskState.FAILED) {
				setState(TaskState.FINISHED);
			}			
		} else {
			setState(TaskState.FINISHED);
		}
		
		onFinish.ifPresent(r -> r.run());
	}

	protected abstract void runTask() throws Exception;
	public abstract SimulationTaskType getType();

}
