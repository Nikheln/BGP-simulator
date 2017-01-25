package bgp.core.fsm;

enum State {
	IDLE,
	CONNECT,
	ACTIVE,
	OPEN_SENT,
	OPEN_CONFIRM,
	ESTABLISHED
}
