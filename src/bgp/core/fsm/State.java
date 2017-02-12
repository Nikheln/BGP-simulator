package bgp.core.fsm;

public enum State {
	IDLE,
	CONNECT,
	ACTIVE,
	OPEN_SENT,
	OPEN_CONFIRM,
	ESTABLISHED
}
