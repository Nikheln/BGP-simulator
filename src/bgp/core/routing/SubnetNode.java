package bgp.core.routing;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgp.simulation.Logger;
import bgp.utils.Subnet;

public class SubnetNode {
	
	protected final Subnet subnet;
	
	protected SubnetNode parent;
	protected final Set<SubnetNode> children;
	
	private int firstHop;
	private int length;
	
	public SubnetNode(SubnetNode parent, Subnet subnet) {
		this.parent = parent;
		this.subnet = subnet;
		this.children = Collections.newSetFromMap(new ConcurrentHashMap<SubnetNode, Boolean>());
		
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
	
	public void setPath(int firstHop, int length) {
		this.firstHop = firstHop;
		this.length = length;
	}
	
	public int getFirstHop() {
		return firstHop;
	}

	public int getLength() {
		return length;
	}

	public void delete() {
		for (SubnetNode child : children) {
			child.setParent(parent);
		}
		parent.children.remove(this);
	}
	
	@Override
	public int hashCode() {
		return subnet.hashCode();
	}
	
	public Iterator<SubnetNode> getChildIterator() {
		return new Iterator<SubnetNode>() {
			private final Queue<SubnetNode> nodes = new ConcurrentLinkedQueue<SubnetNode>(SubnetNode.this.children);
			@Override
			public boolean hasNext() {
				return !nodes.isEmpty();
			}

			@Override
			public SubnetNode next() {
				return nodes.poll();
			}
		};
	}
	
	public Iterator<SubnetNode> getSubnetNodeIterator() {
		return new Iterator<SubnetNode>() {
			private final Queue<SubnetNode> nodes = new ConcurrentLinkedQueue<SubnetNode>(Arrays.asList(SubnetNode.this));
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
