package bgp.core.routing;

import java.util.HashSet;
import java.util.Set;

import bgp.core.Consts;

public class PathSelection {
	private final int firstHop;
	private int localPref;
	private int length;
	
	private final Set<SubnetNode> subnets;
	
	
	public PathSelection(int firstHop, int length) {
		this(firstHop, length, Consts.DEFAULT_PREF);
	}
	
	public PathSelection(int firstHop, int length, int localPref) {
		this.firstHop = firstHop;
		this.length = length;
		this.localPref = localPref;
		this.subnets = new HashSet<>();
	}
	
	public int getLocalPref() {
		return localPref;
	}
	
	public int getLength() {
		return length;
	}
	
	public int getFirstHop() {
		return firstHop;
	}
	
	public void addSubnetNode(SubnetNode n) {
		this.subnets.add(n);
	}

}
