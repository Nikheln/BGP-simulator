package bgp.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import bgp.simulation.Simulator;
import bgp.utils.Pair;

public class NetworkViewer extends SingleGraph {
	
	private final Map<Integer, Node> nodes;
	private final Map<Pair<Integer, Integer>, Edge> edges;
	
	private boolean dirty = false;
	private final ScheduledExecutorService cleanup = Executors.newSingleThreadScheduledExecutor();
	
	public NetworkViewer() {
		super("Router network");
		
		nodes = new HashMap<>();
		edges = new HashMap<>();
		
		setAttribute("layout.quality", 4);
		setAttribute("ui.antialias", 1);
		
		cleanup.scheduleAtFixedRate(() -> {
			if (dirty) {
				updateNetwork();
			}
			dirty = false;
		}, 1, 1, TimeUnit.SECONDS);
	}
	
	public void markAsDirty() {
		dirty = true;
	}
	
	@Override
	public void clear() {
		super.clear();
		nodes.clear();
		edges.clear();
	}

	private void updateNetwork() {
		List<Integer> ids = Simulator.getReservedIds();
		
		// Add routers not in graph
		for (Integer asId : ids) {
			if (!nodes.containsKey(asId)) {
				Node n = addNode(Integer.toString(asId));
				nodes.put(asId, n);
				n.addAttribute("ui.label", Integer.toString(asId));
				n.setAttribute("layout.weight", 2);				
			}
		}
		
		// Remove routers not in network
		for (Iterator<Integer> iter =  nodes.keySet().iterator(); iter.hasNext(); ) {
			int asId = iter.next();
			if (!ids.contains(asId)) {
				for (Iterator<Pair<Integer, Integer>> iter2 = edges.keySet().iterator(); iter2.hasNext();) {
					Pair<Integer, Integer> nextKey = iter2.next();
					if (nextKey.getLeft() == asId || nextKey.getRight() == asId) {
						removeEdge(edges.get(nextKey));
						iter2.remove();
					}
				}
				
				removeNode(nodes.get(asId));
				iter.remove();
			}
		}
		
		// Add edges not in graph
		for (Integer asId : ids) {
			for (Integer otherId : Simulator.getRouter(asId).getConnectedRouterIds()) {
				if (!(edges.containsKey(new Pair<>(asId, otherId)) || edges.containsKey(new Pair<>(otherId, asId)))) {
					Edge e = addEdge(asId + "-" + otherId, nodes.get(asId), nodes.get(otherId));
					edges.put(new Pair<>(asId, otherId), e);
				}
			}
		}
		
		// Remove edges not in network
		for (Iterator<Pair<Integer, Integer>> iter = edges.keySet().iterator(); iter.hasNext(); ) {
			Pair<Integer, Integer> nextKey = iter.next();
			if (!Simulator.getRouter(nextKey.getLeft()).hasConnectionTo(nextKey.getRight())) {
				try {
					removeEdge(edges.get(nextKey));
				} catch (Exception e) {
					
				}
				iter.remove();
			}
		}
	}
	
}
