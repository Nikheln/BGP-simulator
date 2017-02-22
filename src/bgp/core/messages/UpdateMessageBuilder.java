package bgp.core.messages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import bgp.core.messages.pathattributes.PathAttribute;
import bgp.core.network.Subnet;

public class UpdateMessageBuilder {
	
	private final Set<Subnet> withdrawnRoutes = new HashSet<>();
	private final Set<PathAttribute> pathAttributes = new HashSet<>();
	private final Set<Subnet> NLRI = new HashSet<>();
	
	public UpdateMessageBuilder addWithdrawnRoutes(Subnet... subnets) {
		for (Subnet s : subnets) {
			withdrawnRoutes.add(s);
		}
		return this;
	}
	
	public UpdateMessageBuilder addPathAttribute(PathAttribute p) {
		for (PathAttribute pa : pathAttributes) {
			if (pa.getClass().equals(p.getClass())) {
				return this;
			}
		}
		pathAttributes.add(p);
		return this;
	}
	
	public UpdateMessageBuilder addNLRI(Subnet... subnets) {
		for (Subnet s : subnets) {
			NLRI.add(s);
		}
		return this;
	}
	
	public UpdateMessage build() {
		return new UpdateMessage(new ArrayList<>(withdrawnRoutes), new ArrayList<>(pathAttributes), new ArrayList<>(NLRI));
	}

}
