package bgp.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import bgp.core.BGPRouter;
import bgp.core.messages.UpdateMessage;
import bgp.core.messages.notificationexceptions.UpdateMessageException;
import bgp.core.messages.pathattributes.AsPath;
import bgp.core.messages.pathattributes.NextHop;
import bgp.core.messages.pathattributes.Origin;
import bgp.core.messages.pathattributes.PathAttribute;
import bgp.core.network.ASConnection;
import bgp.core.network.fsm.State;
import bgp.simulation.LinkingOrder;
import bgp.simulation.Simulator;
import bgp.utils.PacketEngine;
import bgp.utils.Subnet;

public class BGPRouterTest {

	@Test
	/**
	 * Create and connect two routers, check that
	 * their connection is in state ESTABLISHED afterwards.
	 */
	public void testConnectTwoRouters() {
		Simulator.resetState();
		
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
		
		assertEquals(State.ESTABLISHED, r1.getConnectionFor(r2.id, false).get().getCurrentState());
		assertEquals(State.ESTABLISHED, r2.getConnectionFor(r1.id, false).get().getCurrentState());
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
			BGPRouter r = Simulator.getRouter(i);
			for (ASConnection conn : r.getAllConnections()) {
				assertEquals(State.ESTABLISHED, conn.getCurrentState());
			}
		}
	}
	
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
		BGPRouter r1 = Simulator.getRouter(1);
		BGPClient c1 = new BGPClient(r1);
		
		r1.forwardUpdateMessage(um);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
		
		for (int i = 2; i <= amountOfRouters; i++) {
			assertNotEquals(-1, Simulator.getRouter(i).getRoutingEngine().decidePath(c1.getAddress().getAddress()));
		}
	}
	
	@Test
	public void testPinging() {
		LinkingOrder networkType = LinkingOrder.CLUSTERED;
		int networkRouterCount = 100;
		int clientsPerRouter = 10;
		int pingCount = 10;
		int pingInterval = 200;
		
		buildNetwork(networkType, networkRouterCount);

		List<Integer> routerIds = Simulator.getReservedIds();
		Collections.shuffle(routerIds);
		
		List<Pinger> pingers = new ArrayList<>();
		
		// Add clients to each router
		for (int i = 0; i < clientsPerRouter; i++) {
			PingerClient p = new PingerClient(Simulator.getRouter(routerIds.get(0)));
			Simulator.registerClient(p);
			pingers.add(p);
			List<Long> newClientAddresses = new ArrayList<>();
			
			for (int id : routerIds) {
				BGPClient c = new BGPClient(Simulator.getRouter(id));
				newClientAddresses.add(c.getAddress().getAddress());
				Simulator.registerClient(c);
			}
			
			p.startPinging(newClientAddresses, pingCount, pingInterval);
			Collections.shuffle(routerIds);
		}
		
		try {
			Thread.sleep((pingCount+1)*pingInterval);
		} catch (InterruptedException e) {
		}

		for (Pinger p : pingers) {
			assertEquals(1.0, p.getSuccessRate(), 0.05);
		}
		
	}
	
	@Test
	public void testLinkBreaking() {
		buildNetwork(LinkingOrder.RING, 5);
		
		BGPRouter r3 = Simulator.getRouter(3);
		BGPRouter r4 = Simulator.getRouter(4);
		BGPRouter r5 = Simulator.getRouter(5);
		
		// Make sure the connection works initially
		assertEquals(4, r3.getRoutingEngine().decidePath(r5.getAddress().getAddress()));
		
		// Break the connection R3-R4
		r3.receivePacket(PacketEngine.buildPacket(
				r3.getConnectionFor(4, false).get().getNeighbourAddress(),
				r4.getConnectionFor(3, false).get().getNeighbourAddress(),
				new byte[0]));
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertFalse(r3.getConnectedRouterIds().contains(4));
		
		assertNotEquals(4, r3.getRoutingEngine().decidePath(r5.getAddress().getAddress()));
		assertEquals(2, r3.getRoutingEngine().decidePath(r5.getAddress().getAddress()));
	}
	
	private void buildNetwork(LinkingOrder topology, int amountOfRouters) {
		Simulator.resetState();
		
		for (int i = 1; i <= amountOfRouters; i++) {
			 try {
				 Simulator.registerRouter(new BGPRouter(i, Subnet.getSubnet((10+i)*256*256*256, Subnet.getSubnetMask(16))));
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
			BGPRouter r1 = Simulator.getRouter(id1);
			BGPRouter r2 = Simulator.getRouter(id2);
			
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
			Thread.sleep(amountOfRouters*100);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}

}
