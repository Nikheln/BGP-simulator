package bgp.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import bgp.core.ASConnection;
import bgp.core.network.InterRouterInterface;
import bgp.core.network.packet.PacketRouter;

public class InterASInterfaceTest {

	@Test
	public void test1() {
		TestPacketHandler h1 = new TestPacketHandler();
		TestPacketHandler h2 = new TestPacketHandler();
		InterRouterInterface if1 = new InterRouterInterface(h1, null);
		InterRouterInterface if2 = new InterRouterInterface(h2, null);
		AtomicInteger trueSum1 = new AtomicInteger();
		AtomicInteger trueSum2 = new AtomicInteger();
		AtomicInteger trueCount1 = new AtomicInteger();
		AtomicInteger trueCount2 = new AtomicInteger();
		try {
			if1.connectNeighbourOutputStream(if2);
			if2.connectNeighbourOutputStream(if1);
		} catch (Exception e) {
			fail("Connecting interfaces failed.");
		}
		
		new Thread(if1).start();
		new Thread(if2).start();
		
		for (int i = 0; i < 1000000; i++) {
			byte newValue = (byte)(Math.random()*256);
			trueSum1.addAndGet(newValue);
			trueCount1.incrementAndGet();
			try {
				if1.sendData(new byte[]{newValue});
			} catch (IOException e) {
				fail("Sending data from 1 to 2 failed");
			}

			newValue = (byte)(Math.random()*256);
			trueSum2.addAndGet(newValue);
			trueCount2.incrementAndGet();
			try {
				if2.sendData(new byte[]{newValue});
			} catch (IOException e) {
				fail("Sending data from 2 to 1 failed");
			}
		}
		
		try {
			// Give time for the buffers to empty
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertEquals(trueSum1.get(), h2.getSum());
		assertEquals(trueCount1.get(), h2.getCounterValue());
		assertEquals(trueSum2.get(), h1.getSum());
		assertEquals(trueCount2.get(), h1.getCounterValue());
	}
	
	private class TestPacketHandler implements PacketRouter {
		
		private AtomicInteger counter, summer;
		
		private TestPacketHandler() {
			this.counter = new AtomicInteger();
			this.summer = new AtomicInteger();
		}
		
		private int getCounterValue() {
			return counter.get();
		}
		
		private int getSum() {
			return summer.get();
		}

		@Override
		public void routePacket(byte[] pkg, ASConnection receivingConnection) {
			int value = pkg[0];
			counter.incrementAndGet();
			summer.addAndGet(value);
		}

		@Override
		public void routePacket(byte[] pkg) {
			routePacket(pkg, null);
		}
		
	}

}
