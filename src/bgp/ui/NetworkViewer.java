package bgp.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

import bgp.core.ASConnection;
import bgp.core.BGPRouter;
import bgp.core.SimulatorState;
import bgp.core.fsm.State;
import bgp.core.network.Subnet;

public class NetworkViewer {
	
	private enum LinkingOrder {
		RING,
		STAR,
		RING_STAR,
		RANDOM;
		
		private Queue<Integer> getLinkingOrder(int n) {
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
			case RANDOM:
			default:
				for (int i = 0; i < 4*n; i++) {
					int id = (int) (Math.random()*n + 1);
					list.add(id);
				}
			}

			return list;
		}
	}
	
	public static void main(String[] args) {
		SimulatorState.setTestingMode(true);

		int amountOfRouters = 100;
		
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

		Queue<Integer> ids = LinkingOrder.RING_STAR.getLinkingOrder(amountOfRouters);
		
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
				assertEquals(State.ESTABLISHED, conn.getCurrentState());
			}
		}
		
		SimulatorState.resetState();
	
		Viewer v = g.display();
	}
	
}
