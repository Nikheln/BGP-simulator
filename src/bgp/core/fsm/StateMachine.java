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
	 * @param newState
	 */
	public void changeState(State newState) {
		this.currentState = newState;
	}

}
