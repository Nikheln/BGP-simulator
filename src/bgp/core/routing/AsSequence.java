package bgp.core.routing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bgp.core.Consts;

public class AsSequence {
	private final int[] sequence;
	private int localPref;
	
	private final Set<SubnetNode> subnets;
	
	public AsSequence(List<Integer> sequence) {
		this.sequence = new int[sequence.size()];
		int index = 0;
		for (int id : sequence) {
			this.sequence[index++] = id;
		}
		this.subnets = new HashSet<>();
	}
	
	public AsSequence(int[] sequence) {
		this(sequence, Consts.DEFAULT_PREF);
	}
	
	public AsSequence(int[] sequence, int localPref) {
		this.sequence = sequence;
		this.localPref = localPref;
		this.subnets = new HashSet<>();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof int[]) {
			int[] other = (int[]) obj;
			return Arrays.equals(sequence, other);
		}
		return false;
	}
	
	public int[] getSequence() {
		return sequence;
	}
	
	public int getLocalPref() {
		return localPref;
	}
	
	public int getLength() {
		return sequence.length;
	}
	
	public int getFirstHop() {
		if (sequence.length > 0) {
			return sequence[0];
		} else {
			return -1;
		}
	}
	
	public int getHop(int index) {
		if (index >= sequence.length) {
			return -1;
		} else {
			return sequence[index];
		}
	}
	
	public void addSubnetNode(SubnetNode n) {
		this.subnets.add(n);
	}

}
