package bgp.core.network;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import bgp.core.BGPRouter;
import bgp.core.messages.KeepaliveMessage;
import bgp.core.messages.NotificationMessage;
import bgp.core.messages.OpenMessage;
import bgp.core.network.fsm.State;
import bgp.core.network.fsm.StateMachine;
import bgp.core.trust.TrustEngine;
import bgp.simulation.LogMessage.LogMessageType;
import bgp.simulation.Logger;
import bgp.simulation.SimulatorState;
import bgp.utils.Address;
import bgp.utils.Consts;
import bgp.utils.PacketEngine;

public class ASConnection {
	
	private static final ScheduledExecutorService connectionKeepaliveTimer = Executors.newScheduledThreadPool(4);
	private static ScheduledFuture<?> registerKeepaliveTask(Runnable r, long delay, long interval) {
		return connectionKeepaliveTimer.scheduleAtFixedRate(r, delay, interval, TimeUnit.MILLISECONDS);
	}
	
	private final BGPRouter handler;
	private final InterRouterInterface adapter;
	private final StateMachine fsm;
	
	private boolean hasReceivedKeepalive;
	
	private int retryCounter;
	
	private ScheduledFuture<?> keepaliveChecking, keepaliveSending, retrying;
	
	private int neighbourId;
	private Address ownAddress;
	private Address neighbourAddress;
	
	public ASConnection(Address ownAddress, BGPRouter handler) {
		if (ownAddress == null) {
			throw new IllegalArgumentException("Address can not be null!");
		}
		if (!SimulatorState.isAddressFree(ownAddress)) {
			throw new IllegalStateException("Own address is already reserved");
		}
		
		SimulatorState.reserveAddress(ownAddress);
		
		
		this.adapter = new InterRouterInterface(handler, this);
		this.handler = handler;
		this.fsm = new StateMachine();
		this.fsm.changeState(State.IDLE);
		this.ownAddress = ownAddress;
	}
	
	/**
	 * Start connecting. Initialize the state machine and start sending OPEN messages
	 * @param neighbourAddress
	 */
	public void start(Address neighbourAddress) {
		this.fsm.changeState(State.CONNECT);
		this.retryCounter = 0;
		this.neighbourAddress = neighbourAddress;
		
		this.retrying = registerKeepaliveTask(this::sendOpenMessage, 100, 10000);
	}
	
	private void sendOpenMessage() {
		if (retryCounter < 10) {
			retryCounter++;
			OpenMessage m = new OpenMessage(handler.id,
					Consts.DEFAULT_HOLD_DOWN_TIME,
					ownAddress.getAddress());

			if (fsm.getCurrentState().equals(State.CONNECT)) {
				fsm.changeState(State.OPEN_SENT);
			}
			sendPacket(PacketEngine.buildPacket(ownAddress, neighbourAddress, m.serialize()));
			
		} else {
			retrying.cancel(true);
			closeConnection();
		}
	}
	
	/**
	 * Handle a received OPEN message: start sending and checking
	 * for KEEPALIVE messages and change state to OPEN_CONFIRM
	 * @param m
	 */
	public void handleOpenMessage(OpenMessage m) {
		if (fsm.getCurrentState().equals(State.OPEN_SENT)) {
			neighbourId = m.getASId();
			neighbourAddress = Address.getAddress(m.getBgpId());
			
			// Start checking that KEEPALIVE messages have come
			this.keepaliveChecking = registerKeepaliveTask(() -> {
				if (hasReceivedKeepalive) {
					hasReceivedKeepalive = false;
				} else {
					raiseNotification(NotificationMessage.getHoldTimeExpiredError());
				}
			}, m.getHoldTime()*1000, m.getHoldTime()*1000);

			// Start sending KEEPALIVE messages
			this.keepaliveSending = registerKeepaliveTask(() -> {
				try {
					KeepaliveMessage km = new KeepaliveMessage();
					byte[] packet = PacketEngine.buildPacket(ownAddress, neighbourAddress, km.serialize());
					adapter.sendData(packet);
				} catch (IOException e) {
					// Error sending KEEPALIVE
					raiseNotification(NotificationMessage.getCeaseError());
				}
			}, 50, Consts.DEFAULT_KEEPALIVE_INTERVAL);
			
			
			fsm.changeState(State.OPEN_CONFIRM);
		}
	}
	
	public Address getNeighbourAddress() { 
		return neighbourAddress;
	}
	private static int establisheds = 0;
	public void raiseKeepaliveFlag() {
		if (fsm.getCurrentState().equals(State.OPEN_CONFIRM)) {
			fsm.changeState(State.ESTABLISHED);
			System.out.println(establisheds++);
			retrying.cancel(true);
			handler.sendRoutingInformation(neighbourId);
		}
		this.hasReceivedKeepalive = true;
	}
	
	public boolean isKeepaliveReceived() {
		return hasReceivedKeepalive;
	}
	
	public Address getOwnAddress() {
		return ownAddress;
	}
	
	public State getCurrentState() {
		return fsm.getCurrentState();
	}
	
	public InterRouterInterface getAdapter() {
		return adapter;
	}
	
	public void sendPacket(byte[] packet) {
		try {
			adapter.sendData(packet);
		} catch (IOException e) {
		}
	}
	
	/**
	 * Send a NOTIFICATION message to other end,
	 * possibly change its direct trust, and close the connection
	 * 
	 * @param m
	 */
	public void raiseNotification(NotificationMessage m) {
		// Log the error
		Logger.log("Notification raised to " + neighbourId + ", type: "
				+ m.getErrorType(), handler.id, LogMessageType.CONNECTION);
		// Penalize neighbour in some error cases
		TrustEngine t = handler.getTrustEngine();
		switch (m.getErrorType()) {
		case HOLD_TIMER_EXPIRED:
			t.changeDirectTrust(neighbourId, -10);
			break;
		case MESSAGE_HEADER_ERROR:
			t.changeDirectTrust(neighbourId, -15);
			break;
		case UPDATE_MESSAGE_ERROR:
			t.changeDirectTrust(neighbourId, -20);
			break;
		default:
			break;
		}
		try {
			byte[] message = PacketEngine.buildPacket(ownAddress, neighbourAddress, m.serialize());
			sendPacket(message);
		} catch (Exception e) {
			
		}
		handler.removeConnection(this);
		SimulatorState.refreshNetworkViewer();
	}
	
	/**
	 * Close the connection: shut down the adapter,
	 * stop sending and checking for KEEPALIVEs and change state to IDLE,
	 * tell the router to stop using this adapter
	 */
	public void closeConnection() {
		try {
			adapter.close();
			SimulatorState.releaseAddress(ownAddress);
		} catch (Exception e) {
			// Failed closing the adapter or freeing the address, might be already down
		}
		
		if (this.keepaliveChecking != null) {
			this.keepaliveChecking.cancel(true);
		}
		
		if (this.keepaliveSending != null) {
			this.keepaliveSending.cancel(true);
		}
		
		this.fsm.changeState(State.IDLE);
	}

}
