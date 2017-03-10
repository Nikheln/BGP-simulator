package bgp.simulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public enum LinkingOrder {
	LINE,
	RING,
	STAR,
	RING_STAR,
	CLUSTERED,
	RANDOM;
	
	public Queue<Integer> getLinkingOrder(int n) {
		Queue<Integer> list = new LinkedList<>();
		
		switch (this) {
		case LINE:
			list.add(1);
			for (int i = 1; i <=n; i++) {
				list.add(i);
				if (i != n) {
					list.add(i);
				}
			}
			break;
		case RING:
			list.add(1);
			for (int i = 2; i <= n; i++) {
				list.add(i);
				list.add(i);
			}
			list.add(1);
			break;
		case STAR:
			for (int i = 2; i <= n; i++) {
				list.add(1);
				list.add(i);
			}
			break;
		case RING_STAR:
			for (int i = 2; i <= n; i++) {
				list.add(1);
				list.add(i);
				if (i == n) {
					break;
				}
				list.add(i+1);
				list.add(i);
			}
			list.add(n);
			list.add(2);
			break;
		case CLUSTERED:
			int c = (int) Math.floor(Math.sqrt(n));
			List<Set<Integer>> clusters = new ArrayList<>();
			Set<Integer> latest = null;
			// Generate clusters
			for (int i = 1; i <= n; i++) {
				if ((i-1)%c == 0) {
					latest = new HashSet<>();
					clusters.add(latest);
				}
				latest.add(i);
			}
			
			// Link the cluster members randomly
			int addition = 0;
			for (int i = 0; i < clusters.size(); i++) {
				int size = clusters.get(i).size();
				Queue<Integer> internalLinking = LinkingOrder.RANDOM.getLinkingOrder(size);
				for (int j : internalLinking) {
					list.add(addition + j);
				}
				addition += size;
			}
			
			// Link clusters randomly
			for (int i = 0; i < c*1.5; i++) {
				List<Integer> group1 = new ArrayList<>(clusters.get(i%clusters.size()));
				List<Integer> group2 = group1;
				while (group1 == group2) {
					group2 = new ArrayList<>(clusters.get((int)(Math.random()*c)));
				}
				list.add(group1.get((int)(Math.random()*group1.size())));
				list.add(group2.get((int)(Math.random()*group2.size())));
			}
			break;
		case RANDOM:
		default:
			for (int id = 1; id <= n-2; id++) {
				double rangeLen = (n-id)/2.0;
				int otherId1 = (int) (id + 1 + Math.random()*rangeLen);
				int otherId2 = (int) (id + 1 + rangeLen + Math.random()*rangeLen);
				
				list.add(id);
				list.add(otherId1);
				if (otherId1 != otherId2) {
					list.add(id);
					list.add(otherId2);					
				}
			}
		}

		return list;
	}
}
