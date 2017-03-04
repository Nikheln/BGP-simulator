package bgp.core.network.fsm;

public enum State {
	IDLE,
	CONNECT,
	ACTIVE,
	OPEN_SENT,
	OPEN_CONFIRM,
	ESTABLISHED
}
