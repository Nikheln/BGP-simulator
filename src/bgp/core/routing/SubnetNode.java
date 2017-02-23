package bgp.core.routing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import bgp.core.Consts;
import bgp.core.network.Subnet;

public class SubnetNode {
	
	protected final Subnet subnet;
	
	protected SubnetNode parent;
	protected final Set<SubnetNode> children;
	
	// Set of paths leading to this subnet
	protected final Set<PathSelection> paths;
	
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
	
	public void addPath(PathSelection seq) {
		this.paths.add(seq);
		seq.addSubnetNode(this);
	}
	
	public void deletePath(PathSelection seq) {
		paths.remove(seq);
		if (paths.isEmpty()) {
			delete();
		}
	}
	
	public Set<PathSelection> getPaths() {
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
	
	/**
	 * Removes all paths to this Subnet via the specified neighbour and minimum length
	 * @param firstHop First node in the path
	 * @param maxLength The maximum length for paths to be removed. If a router withdraws a path to some subnet with some sequence length, it could not have existing shorter paths
	 * @return True if the minimum length or maximum pref has changed, or the node has been deleted, false otherwise
	 */
	public boolean removePathsVia(int firstHop, int maxLength) {
		int oldMinLength = getMinLength();
		int oldMaxPref = getMaxPref();
		for (Iterator<PathSelection> iter = paths.iterator(); iter.hasNext();) {
			PathSelection p = iter.next();
			if (p.getFirstHop() == firstHop && p.getLength() <= maxLength) {
				iter.remove();
			}
		}
		if (paths.isEmpty()) {
			delete();
			return true;
		}
		
		int newMinLength = getMinLength();
		int newMaxPref = getMaxPref();
		
		return (newMinLength != oldMinLength) || (newMaxPref != oldMaxPref);
	}
	
	private int getMinLength() {
		return paths.stream().map(ap -> ap.getLength()).sorted((l1, l2) -> l2-l1).findFirst().orElse(255);
	}
	
	private int getMaxPref() {
		return paths.stream().map(ap -> ap.getLocalPref()).sorted((l1, l2) -> l2-l1).findFirst().orElse(Consts.DEFAULT_PREF);
	}

}
