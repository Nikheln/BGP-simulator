package bgp.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import bgp.core.ASConnection;
import bgp.core.BGPRouter;
import bgp.core.SimulatorState;
import bgp.core.fsm.State;
import bgp.core.network.Subnet;

public class NetworkViewer {
	
	public enum LinkingOrder {
		RING,
		STAR,
		RING_STAR,
		CLUSTERED,
		RANDOM;
		
		public Queue<Integer> getLinkingOrder(int n) {
			Queue<Integer> list = new LinkedList<>();
			
			switch (this) {
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
					List<Integer> group1 = new ArrayList<>(clusters.get((int)(Math.random()*c)));
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
					list.add(id);
					list.add(otherId2);
				}
			}

			return list;
		}
	}
	
	public static void main(String[] args) {
		SimulatorState.setTestingMode(true);

		int amountOfRouters = 200;
		LinkingOrder networkType = LinkingOrder.CLUSTERED;
		
		Graph g = new SingleGraph("Router network, n=" + amountOfRouters);
		g.setAttribute("layout.quality", 4);
		g.setAttribute("ui.antialias", 1);
		
		for (int i = 1; i <= amountOfRouters; i++) {
			 try {
				 SimulatorState.registerRouter(new BGPRouter(i, Subnet.getSubnet((10+i)*256*256*256, Subnet.getSubnetMask(16))));
				 Node n = g.addNode(Integer.toString(i));
				 n.addAttribute("ui.label", Integer.toString(i));
				 n.setAttribute("layout.weight", 2);
			} catch (Exception e) {
				fail(e.getMessage());
			}
		}

		Queue<Integer> ids = networkType.getLinkingOrder(amountOfRouters);
		
		while (!ids.isEmpty()) {
			int id1 = ids.poll();
			int id2 = ids.poll();
			
			if (id1 == id2) {
				continue;
			}
			
			BGPRouter r1 = SimulatorState.getRouter(id1);
			BGPRouter r2 = SimulatorState.getRouter(id2);
			
			if (r1.hasConnectionTo(id2)) {
				continue;
			}
			try {
				BGPRouter.connectRouters(r1, r2);
				Edge e = g.addEdge((id1-1)+","+(id2-1), id1-1, id2-1);
				e.addAttribute("layout.weight", 0.1);
			} catch (IllegalArgumentException | IOException e) {
				fail(e.getMessage());
			}
		}
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
		
		for (int i = 1; i <= amountOfRouters; i++) {
			BGPRouter r = SimulatorState.getRouter(i);
			for (ASConnection conn : r.getAllConnections()) {
				//assertEquals(State.ESTABLISHED, conn.getCurrentState());
			}
		}
		
		SimulatorState.resetState();
	
		g.display();
	}
	
	public static void showNetwork() {
		Graph g = new SingleGraph("Router network");
		g.setAttribute("layout.quality", 4);
		g.setAttribute("ui.antialias", 1);
		
		for (Integer asId : SimulatorState.getReservedIds()) {
			Node n = g.addNode(Integer.toString(asId));
			n.addAttribute("ui.label", Integer.toString(asId));
			n.setAttribute("layout.weight", 2);
		}
		
		for (Integer asId : SimulatorState.getReservedIds()) {
			BGPRouter r = SimulatorState.getRouter(asId);
			for (Integer other : r.getConnectedRouterIds()) {
				if (g.getEdge(other + "-" + asId) != null) {
					// Edge already added from other router
					continue;
				}
				g.addEdge(asId + "-" + other, Integer.toString(asId), Integer.toString(other));
			}
		}
		
		g.display();
	}
	
}
