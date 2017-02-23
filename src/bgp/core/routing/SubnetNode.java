package bgp.core.routing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import bgp.core.network.Subnet;

public class SubnetNode {
	
	protected final Subnet subnet;
	
	protected SubnetNode parent;
	protected final Set<SubnetNode> children;
	
	private int firstHop;
	private int localPref;
	private int length;
	
	public SubnetNode(SubnetNode parent, Subnet subnet) {
		this.parent = parent;
		this.subnet = subnet;
		this.children = new HashSet<>();
		
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
	
	public void setPath(int firstHop, int localPref, int length) {
		this.firstHop = firstHop;
		this.localPref = localPref;
		this.length = length;
	}
	
	public int getFirstHop() {
		return firstHop;
	}

	public int getLocalPref() {
		return localPref;
	}

	public int getLength() {
		return length;
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
	
	public Iterator<SubnetNode> getSubnetNodeIterator() {
		return new Iterator<SubnetNode>() {
			private final Queue<SubnetNode> nodes = new LinkedList<>(Arrays.asList(SubnetNode.this));
			@Override
			public boolean hasNext() {
				return !nodes.isEmpty();
			}

			@Override
			public SubnetNode next() {
				SubnetNode next = nodes.poll();
				nodes.addAll(next.children);
				return next;
			}
		};
	}

}
