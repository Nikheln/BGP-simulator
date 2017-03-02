package bgp.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import org.junit.Test;

import bgp.client.BGPClient;
import bgp.client.PingerClient;
import bgp.client.messages.MessageHandlers.Pinger;
import bgp.core.ASConnection;
import bgp.core.BGPRouter;
import bgp.core.SimulatorState;
import bgp.core.fsm.State;
import bgp.core.messages.UpdateMessage;
import bgp.core.messages.notificationexceptions.UpdateMessageException;
import bgp.core.messages.pathattributes.AsPath;
import bgp.core.messages.pathattributes.NextHop;
import bgp.core.messages.pathattributes.Origin;
import bgp.core.messages.pathattributes.PathAttribute;
import bgp.core.network.Subnet;
import bgp.ui.NetworkViewer;
import bgp.ui.NetworkViewer.LinkingOrder;

public class BGPRouterTest {

	@Test
	/**
	 * Create and connect two routers, check that
	 * their connection is in state ESTABLISHED afterwards.
	 */
	public void testConnectTwoRouters() {
		SimulatorState.resetState();
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
	}
	
	@Test
	/**
	 * Create n routers and 8n random connections between them,
	 * check that all connections are in state ESTABLISHED afterwards
	 */
	public void testConnectMultipleRouters() {
		int amountOfRouters = 20;
		buildNetwork(LinkingOrder.RANDOM, amountOfRouters);
		
		for (int i = 1; i <= 20; i++) {
			BGPRouter r = SimulatorState.getRouter(i);
			for (ASConnection conn : r.getAllConnections()) {
				assertEquals(State.ESTABLISHED, conn.getCurrentState());
			}
		}
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testUpdateMessageForwarding() {
		int amountOfRouters = 40;
		buildNetwork(LinkingOrder.CLUSTERED, amountOfRouters);
		
		List<Subnet> withdrawnRoutes = new ArrayList<>();
		List<PathAttribute> pathAttributes = new ArrayList<>();
		try {
			pathAttributes.add(new Origin(2));
			// Next hop can be 0.0.0.0 because it is changed in forwarding phase
			pathAttributes.add(new NextHop(new byte[]{0,0,0,0}));
			pathAttributes.add(new AsPath(new ArrayList<>()));
		} catch (UpdateMessageException e1) {
			fail("Malformed Path attributes");
		}
		List<Subnet> NLRI = new ArrayList<>();
		NLRI.add(Subnet.getSubnet("11.0.0.0/8"));
		
		UpdateMessage um = new UpdateMessage(withdrawnRoutes, pathAttributes, NLRI);
		BGPRouter r1 = SimulatorState.getRouter(1);
		BGPClient c1 = new BGPClient(r1);
		
		r1.forwardUpdateMessage(um);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
		
		for (int i = 2; i <= amountOfRouters; i++) {
			assertNotEquals(-1, SimulatorState.getRouter(i).getRoutingEngine().decidePath(c1.getAddress().getAddress()));
		}
		
		if (false) {
			NetworkViewer.showNetwork();
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testPinging() {
		int networkRouterCount = 40;
		int clientsPerRouter = 4;
		int pingCount = 5;
		int pingInterval = 200;
		
		buildNetwork(LinkingOrder.CLUSTERED, networkRouterCount);

		List<Integer> routerIds = SimulatorState.getReservedIds();
		Collections.shuffle(routerIds);
		
		List<Pinger> pingers = new ArrayList<>();
		
		// Add 4 clients to each router
		for (int i = 0; i < clientsPerRouter; i++) {
			PingerClient p = new PingerClient(SimulatorState.getRouter(routerIds.get(0)));
			SimulatorState.registerClient(p);
			pingers.add(p);
			
			List<Long> newClientAddresses = new ArrayList<>();
			
			for (int id : routerIds) {
				BGPClient c = new BGPClient(SimulatorState.getRouter(id));
				newClientAddresses.add(c.getAddress().getAddress());
				SimulatorState.registerClient(c);
			}
			
			p.startPinging(newClientAddresses, pingCount, pingInterval);
			Collections.shuffle(routerIds);
		}
		
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
		}
		
		for (Pinger p : pingers) {
			assertEquals(1.0, p.getSuccessRate(), 0.01);
		}
	}
	
	private void buildNetwork(LinkingOrder topology, int amountOfRouters) {
		SimulatorState.resetState();
		SimulatorState.setTestingMode(true);
		
		for (int i = 1; i <= amountOfRouters; i++) {
			 try {
				 SimulatorState.registerRouter(new BGPRouter(i, Subnet.getSubnet((10+i)*256*256*256, Subnet.getSubnetMask(16))));
			} catch (Exception e) {
				fail(e.getMessage());
			}
		}

		Queue<Integer> ids = topology.getLinkingOrder(amountOfRouters);		
		
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
	}

}
