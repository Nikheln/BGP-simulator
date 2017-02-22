package bgp.core.routing;

import java.util.HashSet;
import java.util.Set;

import bgp.core.network.Subnet;

public class SubnetNode {
	
	protected final Subnet subnet;
	
	protected SubnetNode parent;
	protected final Set<SubnetNode> children;
	
	// Set of paths leading to this subnet
	protected final Set<AsSequence> paths;
	
	public SubnetNode(SubnetNode parent, Subnet subnet) {
		this.parent = parent;
		this.subnet = subnet;
		this.children = new HashSet<>();
		this.paths = new HashSet<>();
		
		if (parent != null) {
			parent.addChild(this);
		}
	}
	
	public void setParent(SubnetNode parent) {
		this.parent = parent;
	}
	
	public void addChild(SubnetNode child) {
		children.add(child);
	}
	
	public void addPath(AsSequence seq) {
		this.paths.add(seq);
		seq.addSubnetNode(this);
	}
	
	public void deletePath(AsSequence seq) {
		paths.remove(seq);
		if (paths.isEmpty()) {
			delete();
		}
	}
	
	public Set<AsSequence> getPaths() {
		return paths;
	}
	
	public void delete() {
		for (SubnetNode child : children) {
			child.setParent(parent);
		}
	}
	
	@Override
	public int hashCode() {
		return subnet.hashCode();
	}

}
