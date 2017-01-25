package bgp.core.fsm;public class StateMachine {
	
	private State currentState;
	
	public StateMachine() {
		this.currentState = State.IDLE;
	}
	
	public State getCurrentState() {
		return currentState;
	}
	
	/**
	 * Change the machine's state from oldState to newState
	 * @param oldState
	 * @param newState
	 * @throws InvalidParameterException
	 */
	public void changeState(State oldState, State newState) throws IllegalArgumentException {
		if (currentState != oldState) {
			throw new IllegalArgumentException("FSM expected to be in " + oldState + ", was in " + currentState);
		}
		this.currentState = newState;
	}

}
