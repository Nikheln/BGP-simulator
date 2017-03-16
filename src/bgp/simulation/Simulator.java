package bgp.simulation;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import bgp.client.BGPClient;
import bgp.client.messages.MessageHandlers.Pingable;
import bgp.core.BGPRouter;
import bgp.simulation.LogMessage.LogMessageType;
import bgp.simulation.tasks.SimulationTask;
import bgp.simulation.tasks.SimulationTask.TaskState;
import bgp.simulation.tasks.SimulationTask.TopologyChanging;
import bgp.ui.MainView;
import bgp.ui.NetworkViewer;
import bgp.utils.Address;

public class Simulator {
	
	private static final List<Address> usedAddresses = new ArrayList<>();
	private static final Map<Integer, BGPRouter> routers = new ConcurrentHashMap<>();
	private static final Map<Long, BGPClient> clients = new ConcurrentHashMap<>();
	
	private static final Executor clientExecutor = Executors.newFixedThreadPool(8);
	private static final Timer clientTaskTimer = new Timer();
	
	private static Timer simulationTaskTimer = new Timer();
	
	private static long simulationStartTime;
	private static SimulationState state = SimulationState.NOT_STARTED;
	
	public static SimulationState getSimulationState() {
		return state;
	}
	
	public static long getSimulationStartTime() {
		return simulationStartTime;
	}
	
	private static void changeState(SimulationState newState) {
		state = newState;
		stateChangeListeners.forEach(r -> r.run());
	}
	
	private static final List<Runnable> stateChangeListeners = new ArrayList<>();
	
	public static void registerStateChangeListener(Runnable r) {
		stateChangeListeners.add(r);
	}
	
	public enum SimulationState {
		NOT_STARTED,
		STARTED,
		PAUSED,
		FINISHED,
		ERROR;
	}
	
	
	public static void startSimulation(long waitTime, Collection<SimulationTask> tasks) {
		startSimulation(waitTime, tasks, null, null);
	}
	
	private static MainView mainViewer;
	private static NetworkViewer networkViewer;
	
	public static void startSimulation(long waitTime, Collection<SimulationTask> tasks, MainView mv, NetworkViewer viewer) {
		mainViewer = mv;
		networkViewer = viewer;
		resetState();
		simulationTaskTimer = new Timer();
		changeState(SimulationState.STARTED);
		simulationStartTime = System.currentTimeMillis();
		Logger.log("Simulation started", 0, LogMessageType.GENERAL);
		
		long simulationStartMillis = new Date().getTime() + waitTime;
		
		for (SimulationTask t : tasks) {
			if (t.getState() != TaskState.WAITING) {
				continue;
			}
			t.onFinish(() -> {
				if (viewer != null && t instanceof TopologyChanging) {
					refreshNetworkViewer();
				}
			});
			
			Date startTime = new Date(simulationStartMillis + t.getDelay());
			
			if (t.getRepetitions() == 1) {
				simulationTaskTimer.schedule(t, startTime);
			} else {
				simulationTaskTimer.schedule(t, startTime, t.getInterval());
			}
		}
	}
	
	public static void runTaskNow(SimulationTask task) {
		boolean updateView = networkViewer != null && task instanceof TopologyChanging;
		
		task.run();
		
		if (updateView) {
			refreshNetworkViewer();
		}
	}
	
	public static void refreshNetworkViewer() {
		
		if (networkViewer != null) {
			mainViewer.refreshRouterList();
			networkViewer.markAsDirty();
		}
	}
	
	public static void stopSimulation() {
		simulationTaskTimer.cancel();
		resetState();
		Logger.log("Simulation stopped", 0, LogMessageType.GENERAL);
	}
	
	public static void resetState() {
		clients.forEach((adddress, client) -> client.shutdown()); 
		routers.forEach((id, router) -> router.shutdown());
		clients.clear();
		routers.clear();
		
		usedAddresses.clear();
		
		if (networkViewer != null) {
			mainViewer.refreshRouterList();
			networkViewer.clear();
		}

		changeState(SimulationState.NOT_STARTED);
	}
	
	
	
	public static void reserveAddress(Address address) throws IllegalStateException {
		if (!isAddressFree(address)) {
			throw new IllegalStateException("Address " + address + " is already in use.");
		}
		
		usedAddresses.add(address);
	}
	
	public static void releaseAddress(Address toFree) throws IllegalArgumentException {
		int index = -1;
		for (int i = 0; i < usedAddresses.size(); i++) {
			if (usedAddresses.get(i).equals(toFree)) {
				index = i;
			}
		}
		
		if (index >= 0) {
			usedAddresses.remove(index);
		} else {
			throw new IllegalArgumentException("Address is not reserved");
			
		}
		
	}
	
	public static boolean isAddressFree(Address address) {
		for (Address usedAddress : usedAddresses) {
			if (usedAddress.equals(address)) {
				return false;
			}
		}
		return true;
	}
	
	public static void registerRouter(BGPRouter router) throws Exception {
		if (router == null) {
			throw new IllegalArgumentException("Router can not be null");
		}
		if (routers.containsKey(router.id)) {
			throw new IllegalStateException("Router with this id has already been registered");
		}
		
		routers.put(router.id, router);
	}
	
	public static void unregisterRouter(int id) throws IllegalStateException {
		if (!routers.containsKey(id)) {
			throw new IllegalStateException("Router specified is not registered");
		}
		routers.remove(id);
	}
	
	
	public static List<Integer> getReservedIds() {
		return new ArrayList<>(routers.keySet());
	}
	
	public static BGPRouter getRouter(int bgpId) {
		return routers.get(bgpId);
	}
	
	public static Optional<Address> getRouterAddress(int bgpId) {
		return Optional.ofNullable(getRouter(bgpId)).map(BGPRouter::getAddress);
	}
	
	public static void registerClient(BGPClient client) {
		clients.put(client.getAddress().getAddress(), client);
	}
	
	public static void unregisterClient(BGPClient client) {
		clients.remove(client.getAddress().getAddress());
	}
	
	public static Set<Pingable> getPingableClients() {
		return clients.values()
				.stream()
				.filter(c -> c instanceof Pingable)
				.collect(Collectors.toSet());
	}
	
	
	/**
	 * Serve public keys similarly to the way a PKI would
	 * @param bgpId
	 * @return
	 */
	public static byte[] getPublicKey(int bgpId) {
		return Optional.ofNullable(getRouter(bgpId))
				.map(BGPRouter::getPublicKey)
				.map(PublicKey::getEncoded)
				.orElse(new byte[0]);
	}
	
	
	public static Executor getClientExecutor() {
		return clientExecutor;
	}
	
	public static void addClientTask(TimerTask t, long interval) {
		clientTaskTimer.scheduleAtFixedRate(t, interval, interval);
	}
	
}
