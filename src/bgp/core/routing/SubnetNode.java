package bgp.core.routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bgp.core.network.Subnet;

public class SubnetNode {
	
	protected SubnetNode parent;
	protected final List<SubnetNode> children;
	
	protected final Subnet subnet;
	
	protected final Set<ASNode> asSet;
	
	
	public SubnetNode(SubnetNode parent, Subnet subnet) {
		this(subnet);
		this.parent.linkChildNode(this);
	}
	
	public SubnetNode(Subnet subnet) {
		this.subnet = subnet;
		this.asSet = new HashSet<>();
		this.children = new ArrayList<>();
	}
	
	/**
	 * Delete this Node from the graph, linking this' {@link #children} to {@link #parent}
	 */
	public void delete() {
		for (SubnetNode n : children) {
			parent.linkChildNode(n);
		}
		for (ASNode n : asSet) {
			n.removeContainedSubnet(this);
		}
		children.clear();
		asSet.clear();
	}
	
	/**
	 * Delete information about routing packets into this subnet towards the specified BGP router.
	 * If this empties the router list, delete this node.
	 * @param bgpId
	 */
	public void removeRouter(ASNode bgpId) {
		asSet.remove(bgpId);
		bgpId.removeContainedSubnet(this);
		if (asSet.isEmpty()) {
			delete();
		}
	}
	
	/**
	 * Link a new child node to this node, setting this to child's
	 * parent and adding child to this' list of children.
	 * @param child
	 */
	public void linkChildNode(SubnetNode child) {
		children.add(child);
		child.parent = this;
	}
	
	public void linkContainingAS(ASNode node) {
		asSet.add(node);
		node.addContainedSubnet(this);
	}
	
	
}