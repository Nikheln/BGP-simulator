package bgp.simulation.tasks;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import bgp.client.PingerClient;
import bgp.core.BGPRouter;
import bgp.simulation.Simulator;

public class StartGeneratingTrafficTask extends SimulationTask {

	private final int sourceRouter;
	private final List<Integer> destinationRouters;
	private final long frequency;
	private PingerClient pinger;
	
	public StartGeneratingTrafficTask(int sourceRouter, int destinationRouter, long frequency, long delay) {
		this(sourceRouter, Arrays.asList(destinationRouter), frequency, delay);
	}
	
	public StartGeneratingTrafficTask(int sourceRouter, List<Integer> destinationRouters, long frequency, long delay) {
		super(1, 0, delay);
		this.sourceRouter = sourceRouter;
		this.destinationRouters = destinationRouters;
		this.frequency = frequency;
	}
	
	public Optional<PingerClient> getPingerClient() {
		return Optional.ofNullable(pinger);
	}
	
	@Override
	public String toString() {
		return sourceRouter + " -> "
				+ destinationRouters.stream().map(id -> id.toString()).collect(Collectors.joining(", "))
				+ " (" + frequency + " ms)";
	}

	@Override
	protected void runTask() throws Exception {
		BGPRouter r = Simulator.getRouter(sourceRouter);
		if (r == null) {
			throw new Exception();
		}
		pinger = new PingerClient(r);
		List<Long> recipientAddresses = (destinationRouters.isEmpty() ? Simulator.getReservedIds() : destinationRouters)
				.stream()
				// Map ID's to routers
				.map(id -> Simulator.getRouter(id))
				// Filter unfound routers
				.filter(router -> router != null)
				// Extract clients as PacketReceiver's from the routers
				.flatMap(router -> router.getClients().stream())
				// Map the clients to their addresses (as long)
				.map(pr -> pr.getAddress().getAddress())
				// Collect the addresses to a list
				.collect(Collectors.toList());
		
		pinger.startPinging(recipientAddresses, frequency);
	}

	@Override
	public SimulationTaskType getType() {
		return SimulationTaskType.START_GENERATING_TRAFFIC;
	}

}
