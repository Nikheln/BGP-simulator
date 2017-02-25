package bgp.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import bgp.core.messages.UpdateMessage;
import bgp.core.messages.notificationexceptions.NotificationException;
import bgp.core.messages.pathattributes.AsPath;
import bgp.core.messages.pathattributes.NextHop;
import bgp.core.messages.pathattributes.Origin;
import bgp.core.messages.pathattributes.PathAttribute;
import bgp.core.network.Address;
import bgp.core.network.Subnet;
import bgp.core.routing.RoutingEngine;
import bgp.core.trust.TrustEngine;

public class RoutingEngineTest {

	@Test
	public void testHandleUpdateMessage() {
		try {
			RoutingEngine e1 = new RoutingEngine(1, new TrustEngine());
			RoutingEngine e2 = new RoutingEngine(2, new TrustEngine());
			RoutingEngine e3 = new RoutingEngine(3, new TrustEngine());
			
			// Initial message from E1 to E2
			List<Subnet> withdrawnRoutes1 = new ArrayList<>();
			List<PathAttribute> pathAttributes1 = new ArrayList<>();
			pathAttributes1.add(new Origin(2));
			pathAttributes1.add(new NextHop(Address.getAddress("10.0.0.1").getBytes()));
			pathAttributes1.add(new AsPath(Arrays.asList(1)));
			List<Subnet> NLRI1 = new ArrayList<>();
			NLRI1.add(Subnet.getSubnet("10.0.0.0/16"));
			NLRI1.add(Subnet.getSubnet("10.1.0.0/16"));
			NLRI1.add(Subnet.getSubnet("10.128.0.0/20"));
			UpdateMessage um1 = new UpdateMessage(withdrawnRoutes1, pathAttributes1, NLRI1);
			e2.handleUpdateMessage(um1);
			
			// Initial message from E2 to E3
			List<Subnet> withdrawnRoutes2 = new ArrayList<>();
			List<PathAttribute> pathAttributes2 = new ArrayList<>();
			pathAttributes2.add(new Origin(2));
			pathAttributes2.add(new NextHop(Address.getAddress("20.0.0.1").getBytes()));
			pathAttributes2.add(new AsPath(Arrays.asList(2)));
			List<Subnet> NLRI2 = new ArrayList<>();
			NLRI2.add(Subnet.getSubnet("20.0.0.0/16"));
			NLRI2.add(Subnet.getSubnet("20.1.0.0/16"));
			NLRI2.add(Subnet.getSubnet("20.128.0.0/20"));
			UpdateMessage um2 = new UpdateMessage(withdrawnRoutes2, pathAttributes2, NLRI2);
			e3.handleUpdateMessage(um2);
			
			// Routed message E1-E2-E3
			List<Subnet> withdrawnRoutes3 = new ArrayList<>();
			List<PathAttribute> pathAttributes3 = new ArrayList<>();
			pathAttributes3.add(new Origin(2));
			pathAttributes3.add(new NextHop(Address.getAddress("20.0.0.1").getBytes()));
			pathAttributes3.add(new AsPath(Arrays.asList(2,1)));
			List<Subnet> NLRI3 = new ArrayList<>();
			NLRI3.add(Subnet.getSubnet("10.0.0.0/16"));
			NLRI3.add(Subnet.getSubnet("10.1.0.0/16"));
			NLRI3.add(Subnet.getSubnet("10.128.0.0/20"));
			UpdateMessage um3 = new UpdateMessage(withdrawnRoutes3, pathAttributes3, NLRI3);
			e3.handleUpdateMessage(um3);
			
			// Initial message from E3 to E2
			List<Subnet> withdrawnRoutes4 = new ArrayList<>();
			List<PathAttribute> pathAttributes4 = new ArrayList<>();
			pathAttributes4.add(new Origin(2));
			pathAttributes4.add(new NextHop(Address.getAddress("30.0.0.1").getBytes()));
			pathAttributes4.add(new AsPath(Arrays.asList(3)));
			List<Subnet> NLRI4 = new ArrayList<>();
			NLRI4.add(Subnet.getSubnet("30.0.0.0/16"));
			NLRI4.add(Subnet.getSubnet("30.1.0.0/16"));
			NLRI4.add(Subnet.getSubnet("30.128.0.0/20"));
			UpdateMessage um4 = new UpdateMessage(withdrawnRoutes4, pathAttributes4, NLRI4);
			e2.handleUpdateMessage(um4);
			um4.appendOwnId(2);
			e1.handleUpdateMessage(um4);
			
			assertEquals(2, e3.decidePath(Address.getAddress("10.0.10.5").getAddress()));
			// No valid subnet, should be dropped
			assertEquals(-1, e3.decidePath(Address.getAddress("20.4.150.7").getAddress()));
			
			assertEquals(3, e2.decidePath(Address.getAddress("30.0.10.1").getAddress()));
			assertEquals(2, e1.decidePath(Address.getAddress("30.0.10.1").getAddress()));
			assertEquals(1, e2.decidePath(Address.getAddress("10.0.1.5").getAddress()));
			
			// E3 withdraws subnets
			List<Subnet> withdrawnRoutes5 = new ArrayList<>();
			withdrawnRoutes5.add(Subnet.getSubnet("3.0.0.0/16"));
			withdrawnRoutes5.add(Subnet.getSubnet("30.128.0.0/20"));
			List<PathAttribute> pathAttributes5 = new ArrayList<>();
			pathAttributes5.add(new Origin(2));
			pathAttributes5.add(new NextHop(Address.getAddress("30.0.0.1").getBytes()));
			pathAttributes5.add(new AsPath(Arrays.asList(3)));
			List<Subnet> NLRI5 = new ArrayList<>();
			UpdateMessage um5 = new UpdateMessage(withdrawnRoutes5, pathAttributes5, NLRI5);
			e2.handleUpdateMessage(um5);
			um5.appendOwnId(2);
			um5.changeNextHop(Address.getAddress("20.0.0.1").getBytes());
			e1.handleUpdateMessage(um5);
			
			assertEquals(-1, e2.decidePath(Address.getAddress("30.128.15.5").getAddress()));
			assertEquals(-1, e1.decidePath(Address.getAddress("30.128.15.5").getAddress()));
		} catch (NotificationException e) {
			fail(e.buildNotification().toString());
		}
	}

}
