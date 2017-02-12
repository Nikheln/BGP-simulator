package bgp.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.Test;

import bgp.core.ASConnection;
import bgp.core.BGPRouter;
import bgp.core.SimulatorState;
import bgp.core.fsm.State;
import bgp.core.network.Subnet;

public class BGPRouterTest {

	@Test
	/**
	 * Create and connect two routers, check that
	 * their connection is in state ESTABLISHED afterwards.
	 */
	public void testConnectTwoRouters() {
		SimulatorState.setTestingMode(true);
		
		BGPRouter r1 = new BGPRouter(100, Subnet.getSubnet("10.0.0.0/8"));
		BGPRouter r2 = new BGPRouter(101, Subnet.getSubnet("11.0.0.0/8"));
		
		try {
			BGPRouter.connectRouters(r1, r2);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
		
		assertEquals(State.ESTABLISHED, r1.getConnectionFor(r2.id).getCurrentState());
		assertEquals(State.ESTABLISHED, r2.getConnectionFor(r1.id).getCurrentState());
		
		SimulatorState.resetState();
	}
	
	@Test
	/**
	 * Create n routers and 8n random connections between them,
	 * check that all connections are in state ESTABLISHED afterwards
	 */
	public void testConnectMultipleRouters() {
		SimulatorState.setTestingMode(true);
		int amountOfRouters = 20;
		
		for (int i = 1; i <= amountOfRouters; i++) {
			 try {
				 SimulatorState.registerRouter(new BGPRouter(i, Subnet.getSubnet((10+i)*256*256*256, Subnet.getSubnetMask(16))));
			} catch (Exception e) {
				fail(e.getMessage());
			}
		}

		Queue<Integer> ids = new LinkedList<>();
		for (int i = 0; i < amountOfRouters*8; i++) {
			ids.add(1+(int)(Math.random()*(amountOfRouters-1)));
		}
		
		
		while (!ids.isEmpty()) {
			int id1 = ids.poll();
			int id2 = ids.poll();
			if (id1 == id2) {
				continue;
			}
			BGPRouter r1 = SimulatorState.getRouter(id1);
			BGPRouter r2 = SimulatorState.getRouter(id2);
			
			if (r1.hasConnectionTo(id2)) {
				continue;
			}
			try {
				BGPRouter.connectRouters(r1, r2);
			} catch (IllegalArgumentException | IOException e) {
				fail(e.getMessage());
			}
		}
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
		
		for (int i = 1; i <= 20; i++) {
			BGPRouter r = SimulatorState.getRouter(i);
			for (ASConnection conn : r.getAllConnections()) {
				assertEquals(State.ESTABLISHED, conn.getCurrentState());
			}
		}
		
		SimulatorState.resetState();
	}

}
