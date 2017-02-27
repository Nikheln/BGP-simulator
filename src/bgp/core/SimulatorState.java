package bgp.core;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bgp.core.network.Address;

public class SimulatorState {
	
	public static boolean testingMode = false;
	
	private static final List<Address> usedAddresses = new ArrayList<>();
	private static final Map<Integer, BGPRouter> routers = new HashMap<>();
	
	public static void resetState() {
		usedAddresses.clear();
		routers.clear();
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
	
	public static BGPRouter getRouter(int bgpId) {
		return routers.get(bgpId);
	}
	
	/**
	 * Serve public keys similarly to a PKI would
	 * @param bgpId
	 * @return
	 */
	public static PublicKey getPublicKey(int bgpId) {
		return getRouter(bgpId).getPublicKey();
	}
	
	public static Set<Integer> getReservedIds() {
		return routers.keySet();
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
	
}
