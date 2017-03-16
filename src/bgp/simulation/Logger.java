package bgp.simulation;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import bgp.simulation.LogMessage.LogMessageType;
import bgp.utils.Address;

public class Logger {
	
	private static Executor logHandler = Executors.newSingleThreadExecutor();
	private static Optional<Consumer<LogMessage>> logFunction = Optional.empty();
	
	/**
	 * Set a function to be run every time a logged event is available.
	 * @param r
	 */
	public static void setLogHandler(Consumer<LogMessage> r) {
		logFunction = Optional.ofNullable(r);
	}

	private static final void processLog(LogMessage lm) {
		logFunction.ifPresent(f -> {
			logHandler.execute(() -> {
				f.accept(lm);
			});
		});
	}
	
	public static void log(String message, int routerId, LogMessageType type) {
		processLog(new LogMessage(routerId, message, type));
	}
	
	public static void clientLog(String message, Address address, LogMessageType type) {
		processLog(new ClientLogMessage(address, message, type));
	}
}
