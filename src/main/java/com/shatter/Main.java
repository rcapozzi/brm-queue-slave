package com.shatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import org.junit.Test;

import com.portal.pcm.EBufException;
import com.portal.pcm.FList;
import com.portal.pcm.PCPSelector;
import com.portal.pcm.Poid;
import com.portal.pcm.PortalContext;
import com.portal.pcm.PortalContextListener;
import com.portal.pcm.PortalOp;
import com.portal.pcm.fields.FldPoid;
import com.shatter.demo.DemoPortalContextListener;
import com.shatter.demo.DemoPCPSelector;

public class Main {
	static final Logger LOG = Logger.getLogger("Main");

	@Test
	public static void main(String[] args) {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
		// 2021-02-20 09:55:13 INFO DemoPCPSelector run return
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %5$s%6$s%n");

		System.out.println("Hello thread=" + Thread.currentThread().getId());
//		callSyncOpcode();
//		callASyncOpcode();
		demoASyncPool();

//		selectOpcode();
		System.out.println("exit");
	}

	public static void callASyncOpcode() {
		LOG.info("callASyncOpcode enter");
		PCPSelector pcpSelector = null;
		pcpSelector = DemoPCPSelector.getInstance();
		LOG.info("created selector ");
		DemoPortalContextListener pListener = new DemoPortalContextListener();
		pListener.setPCPSelector(pcpSelector);

		Thread t = new Thread((Runnable) pcpSelector);
		t.start();

		PortalContext portalContext = null;
		try {
			portalContext = new PortalContext(pcpSelector);
			LOG.info("created portalContext with pcpSelector " + pcpSelector);
			portalContext.connect();
			LOG.info("portalContext connected");
		} catch (EBufException e) {
			LOG.severe("Error creating PCPSelector");
			throw new RuntimeException(e);
		}

		Poid poid = Poid.valueOf("0.0.0.1 /account 1");
		FList request = null;
		request = new FList();
		request.set(FldPoid.getInst(), poid);

		try {
			FList response = portalContext.opcode(PortalOp.GET_PIN_VIRTUAL_TIME, request);
			LOG.info("Called opcode:\n" + response);
		} catch (EBufException e) {
			LOG.severe("Error calling opcode");
		}

		try {
			portalContext.opcodeSend(PortalOp.GET_PIN_VIRTUAL_TIME, request);
			LOG.info("sent request");
		} catch (EBufException e) {
			LOG.severe("Error calling opcodeSend");
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		try {
			portalContext.close(true);
			pcpSelector.shutdown();
			Thread.sleep(500);
			portalContext.close(true);
		} catch (InterruptedException e) {
			LOG.severe("Caught InterruptedException exception");
			e.printStackTrace();
		} catch (EBufException e) {
			LOG.severe("Error calling opcodeSend");
			e.printStackTrace();
		}
	}

	public static void callSyncOpcode() {
		System.out.println("callOpcode enter");
		LOG.info("callOpcode enter");
		PortalContext portalContext = null;
		try {
			portalContext = new PortalContext();
			LOG.info("callOpcode connecting");
			portalContext.connect();
		} catch (EBufException e) {
//			e.printStackTrace();
			throw new RuntimeException(e);
		}
		Poid poid = Poid.valueOf("0.0.0.1 /account 1");
		FList response = null;
		FList request = new FList();
		request.set(FldPoid.getInst(), poid);

		try {
			response = portalContext.opcode(PortalOp.GET_PIN_VIRTUAL_TIME, request);
			LOG.info("response:\n" + response);
			portalContext.close(true);
		} catch (EBufException e) {
//			e.printStackTrace();
			throw new RuntimeException(e);
		}
		LOG.info("callOpcode return");
	}

	/**
	 * Self manage an array of connections. Cons: How to trigger a search for the
	 * next availible context? Wait for the next incoming message and then iterate
	 * over the list?
	 */
	public static void selectOpcode() {
		LOG.info("callASyncOpcode enter");
		PCPSelector pcpSelector = null;
		pcpSelector = DemoPCPSelector.getInstance();
		LOG.info("created selector ");
		PortalContext portalContext = null;

		int POOL_SIZE = 4;
		PortalContext[] portalContexts = new PortalContext[POOL_SIZE];

		try {
			for (int i = 0; i < POOL_SIZE; i++) {
				portalContext = new PortalContext(pcpSelector);
				LOG.info("created portalContext with pcpSelector");

				portalContext.connect();
				LOG.info("portalContext connected");
				portalContexts[i] = portalContext;
			}

		} catch (EBufException e) {
			LOG.severe("Error creating PortalContext");
			throw new RuntimeException(e);
		}

		Poid poid = Poid.valueOf("0.0.0.1 /account 1");
		FList request = null;
		try {
			request = new FList();
			request.set(FldPoid.getInst(), poid);

			for (int i = 0; i < POOL_SIZE; i++) {
				portalContext = portalContexts[i];
				System.out.println(String.format("%d: isSocketChannel Connected=%s Open=%s Registered=%s", i,
						portalContext.isSocketChannelConnected(), portalContext.isSocketChannelOpen(),
						portalContext.isSocketChannelRegistered()));
				portalContext.opcodeSend(PortalOp.GET_PIN_VIRTUAL_TIME, request);
				LOG.info("sent request");
			}

		} catch (EBufException e) {
			LOG.severe("Error calling opcodeSend");
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		for (int i = 0; i < POOL_SIZE; i++) {
			portalContext = portalContexts[i];
			System.out.println(String.format("%d: isSocketChannel Connected=%s Open=%s Registered=%s", i,
					portalContext.isSocketChannelConnected(), portalContext.isSocketChannelOpen(),
					portalContext.isSocketChannelRegistered()));
			try {
				portalContext.opcodeReceive();
				portalContext.opcodeReceive();

			} catch (EBufException e) {
				System.out.println("Bad opcodeReceive");
			}
			System.out.println(String.format("%d: isSocketChannel Connected=%s Open=%s Registered=%s", i,
					portalContext.isSocketChannelConnected(), portalContext.isSocketChannelOpen(),
					portalContext.isSocketChannelRegistered()));
		}
	}


	public static int expandPool() {
		PCPSelector pcpSelector = null;
		pcpSelector = DemoPCPSelector.getInstance();
		PortalContext portalContext;
		try {
			portalContext = new PortalContext(pcpSelector);
			portalContext.connect();
			pool.add(portalContext);
			POOL_SIZE_CURRENT += 1;
		} catch (EBufException e) {
			LOG.severe("Error creating portalContext");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		LOG.info("Added portalContext to pool. Size now " + POOL_SIZE_CURRENT);

		return POOL_SIZE_CURRENT;
	}

	static int POOL_SIZE_INITIAL = 2;
	static int POOL_SIZE_CURRENT = 0;
	static ArrayBlockingQueue<PortalContext> pool = new ArrayBlockingQueue<PortalContext>(128);

	/**
	 * Demo a pooled set of connections being used async
	 */
	public static void demoASyncPool() {
		PortalContext portalContext = null;
		long tHealthCheckIntervalSeconds = 60;
		long tNextHealthCheck = -1;
		long tNow = -1;

		LOG.info("callASyncPool enter");
		int MAX_LOOPS = 16;

		PCPSelector pcpSelector = null;
		pcpSelector = DemoPCPSelector.getInstance();
		LOG.info("created DemoPCPSelector " + pcpSelector.hashCode());

		DemoPortalContextListener pListener = new DemoPortalContextListener();
		pListener.setPCPSelector(DemoPCPSelector.getInstance());
		pListener.setPool(pool);

		Thread t = new Thread((Runnable) pcpSelector);
		t.start();

		while (POOL_SIZE_CURRENT < POOL_SIZE_INITIAL) {
			expandPool();
		}

		Poid poid = Poid.valueOf("0.0.0.1 /account 1");
		FList request = null;

		// Main Loop
		try {
			request = new FList();
			request.set(FldPoid.getInst(), poid);

			for (int i = 0; i < MAX_LOOPS; i++) {
				tNow = System.nanoTime();
				if (tNextHealthCheck < tNow || i % 4 == 0) {
					int min = 2;
					int max = 16;
					int newSize = (int) ((Math.random() * ((max - min) + 1)) + min);
					
					LOG.info("Perform health check. loop=" + i + " targetSize=" + newSize);
					resizePool(newSize);
					tNextHealthCheck = +(tNow + (tHealthCheckIntervalSeconds * 1000000000));
				}

				portalContext = pool.take();
				portalContext.opcodeSend(PortalOp.GET_PIN_VIRTUAL_TIME, request);
				long elapsedMS = (System.nanoTime() - tNow) / 1000000;

				LOG.info(String.format("sent request #%d poolSize=%s pc=%010d", i,
						pool.size(), portalContext.hashCode()));
			}

		} catch (InterruptedException e) {
			LOG.severe("Error getting portalContext from pool");
			throw new RuntimeException(e);
		} catch (EBufException e) {
			LOG.severe("Error calling opcodeSend");
			throw new RuntimeException(e);
		}

		// Close every connection we opened. Blocks until complete.
		LOG.info("Shutting down");
		while (POOL_SIZE_CURRENT > 0) {
			shrinkPool();
		}

		pcpSelector.shutdown();
		LOG.info("Shut down complete");
	}

	static void resizePool(int targetSize) {
		while (POOL_SIZE_CURRENT < targetSize) {
			expandPool();
		}

		while (POOL_SIZE_CURRENT > targetSize) {
			shrinkPool();
		}
	}

	public static int shrinkPool() {
		PortalContext portalContext;
		try {
			portalContext = pool.take();
			portalContext.close(true);
			POOL_SIZE_CURRENT -= 1;
		} catch (EBufException e) {
			LOG.severe("Error closing portalContext");
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			LOG.severe("Error taking portalContext");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		LOG.info("Closed portalContext. Pool size now " + POOL_SIZE_CURRENT);
		return POOL_SIZE_CURRENT;
	}


}
/**
 * PCPSelector sends the notification to PortalContextListener once the data has
 * arrived extend the PCPSelector class and implement its Runnable interface
 * 
 * PortalContextListener.handlePortalContextEvent() supplies the PortalContext
 * object with the related event that has occurred recently. When this data is
 * available, the handlePortalContextEvent method of the PortalContextListener
 * object in your client application is called automatically.
 */
