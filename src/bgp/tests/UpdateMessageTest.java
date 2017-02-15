package bgp.tests;

import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import bgp.core.messages.BGPMessage;
import bgp.core.messages.UpdateMessage;
import bgp.core.messages.pathattributes.AsPath;
import bgp.core.messages.pathattributes.NextHop;
import bgp.core.messages.pathattributes.Origin;
import bgp.core.messages.pathattributes.PathAttribute;
import bgp.core.network.Address;
import bgp.core.network.Subnet;

public class UpdateMessageTest {

	@Test
	/**
	 * Test UPDATE message serialization and deserialization.
	 */
	public void testGetBody() {
		List<Subnet> withdrawnRoutes = new ArrayList<>();
		withdrawnRoutes.add(Subnet.getSubnet("10.0.0.0/8"));
		withdrawnRoutes.add(Subnet.getSubnet("10.250.0.0/16"));
		withdrawnRoutes.add(Subnet.getSubnet("10.17.25.0/24"));
		withdrawnRoutes.add(Subnet.getSubnet("10.0.150.64/30"));
		
		List<PathAttribute> pathAttributes = new ArrayList<>();
		pathAttributes.add(new Origin(1));
		pathAttributes.add(new NextHop(Address.getAddress("132.25.67.101").getBytes()));
		pathAttributes.add(new AsPath(Arrays.asList(1,2,3,4,5,6,7)));
		
		
		List<Subnet> NLRI = new ArrayList<>();
		NLRI.add(Subnet.getSubnet("10.0.0.0/8"));
		NLRI.add(Subnet.getSubnet("10.250.0.0/16"));
		NLRI.add(Subnet.getSubnet("10.17.25.0/24"));
		NLRI.add(Subnet.getSubnet("10.0.150.64/30"));
		
		
		UpdateMessage original = new UpdateMessage(withdrawnRoutes, pathAttributes, NLRI);
		byte[] body = original.serialize();
		UpdateMessage deserialized = (UpdateMessage) BGPMessage.deserialize(body);
		
		List<Subnet> dWithdrawnRoutes = deserialized.getWithdrawnRoutes();
		List<PathAttribute> dPathAttributes = deserialized.getPathAttributes();
		List<Subnet> dNLRI = deserialized.getNLRI();
		
		assertThat(dWithdrawnRoutes, CoreMatchers.hasItems(withdrawnRoutes.toArray(new Subnet[withdrawnRoutes.size()])));
		assertThat(dPathAttributes, CoreMatchers.hasItems(pathAttributes.toArray(new PathAttribute[pathAttributes.size()])));
		assertThat(dNLRI, CoreMatchers.hasItems(NLRI.toArray(new Subnet[NLRI.size()])));
	}

}
