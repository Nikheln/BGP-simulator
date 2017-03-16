package bgp.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import bgp.simulation.LinkingOrder;
import bgp.simulation.Simulator;
import bgp.simulation.tasks.AddClientsTask;
import bgp.simulation.tasks.ConnectRoutersTask;
import bgp.simulation.tasks.DisconnectRoutersTask;
import bgp.simulation.tasks.GenerateNetworkTask;
import bgp.simulation.tasks.SimulationTask;
import bgp.simulation.tasks.SimulationTask.TaskState;
import bgp.simulation.tasks.StartGeneratingTrafficTask;
import bgp.simulation.tasks.StopGeneratingTrafficTask;
import bgp.ui.NetworkViewer;

public class SimulationTaskTest {

	@Test
	public void test() {
		List<SimulationTask> tasks = new ArrayList<>();
		tasks.add(new GenerateNetworkTask(LinkingOrder.CLUSTERED, 30));
		tasks.add(new ConnectRoutersTask(3, 25, 1000));
		tasks.add(new AddClientsTask(Arrays.asList(5), 10, 2000));
		tasks.add(new AddClientsTask(Arrays.asList(14), 10, 2000));
		StartGeneratingTrafficTask t1 = new StartGeneratingTrafficTask(1, 5, 1, 4000);
		tasks.add(t1);
		StartGeneratingTrafficTask t2 = new StartGeneratingTrafficTask(2, 14, 1, 4000);
		tasks.add(t2);
		tasks.add(new DisconnectRoutersTask(3, 25, 8000));
		tasks.add(new StopGeneratingTrafficTask(t1, 10000));
		tasks.add(new StopGeneratingTrafficTask(t2, 10000));
		
		
		
		NetworkViewer n = new NetworkViewer();
		n.display();
		Simulator.startSimulation(3000, tasks, null, n);
		
		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
		
		for (SimulationTask t : tasks) {
			assertEquals(TaskState.FINISHED, t.getState());
		}
		assertTrue(t1.getPingerClient().map(c -> c.getSuccessRate()).orElse(0.0) >= 0.9);
		assertTrue(t2.getPingerClient().map(c -> c.getSuccessRate()).orElse(0.0) >= 0.9);
		
	}

}
