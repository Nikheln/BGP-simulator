package bgp.simulation;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import bgp.client.BGPClient;
import bgp.client.messages.MessageHandlers.Pingable;
import bgp.core.BGPRouter;
import bgp.simulation.tasks.SimulationTask;
import bgp.utils.Address;

public class SimulatorState {
	
	public static boolean testingMode = false;
	
	private static final List<Address> usedAddresses = new ArrayList<>();
	private static final Map<Integer, BGPRouter> routers = new HashMap<>();
	private static final Map<Long, BGPClient> clients = new HashMap<>();
	
	private static final Executor clientExecutor = Executors.newFixedThreadPool(8);
	private static final Timer clientTaskTimer = new Timer();
	
	private static final Timer simulationTaskTimer = new Timer();
	
	public static void startSimulation(int waitTime, Collection<SimulationTask> tasks) {
		resetState();
		
		long simulationStartMillis = new Date().getTime() + waitTime;
		
		for (SimulationTask t : tasks) {
			Date startTime = new Date(simulationStartMillis + t.getDelay());
			
			if (t.getRepetitions() == 1) {
				simulationTaskTimer.schedule(t, startTime);
			} else {
				simulationTaskTimer.schedule(t, startTime, t.getInterval());
			}
		}
	}
	
	public static void resetState() {
		usedAddresses.clear();
		routers.clear();
		clients.clear();
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
	
	public static void setTestingMode(boolean tMode) {
		testingMode = tMode;
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
	
	public static void unregisterRouter(BGPRouter router) throws Exception {
		if (router == null) {
			throw new IllegalArgumentException("Router removed can not be null");
		}
		unregisterRouter(router.id);
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
	
	public static Address getRouterAddress(int bgpId) {
		return getRouter(bgpId).getAddress();
	}
	
	public static void registerClient(BGPClient client) {
		clients.put(client.getAddress().getAddress(), client);
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
	public static PublicKey getPublicKey(int bgpId) {
		return getRouter(bgpId).getPublicKey();
	}
	
	
	public static Executor getClientExecutor() {
		return clientExecutor;
	}
	
	public static void addClientTask(TimerTask t, int interval) {
		clientTaskTimer.scheduleAtFixedRate(t, interval, interval);
	}
	
}
