package com.shatter.demo;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;
import com.portal.pcm.EBufException;
import com.portal.pcm.FList;
import com.portal.pcm.PCPSelector;
import com.portal.pcm.PortalContext;
import com.portal.pcm.PortalContextListener;

public class DemoPortalContextListener extends PortalContextListener {
	static final Logger LOG = Logger.getLogger("DemoPortalContextListener");
	ArrayBlockingQueue<PortalContext> pool = null;

//	public DemoPortalContextListener() {
//		PCPSelector pcpSelector = null;
//		pcpSelector = DemoPCPSelector.getInstance();
//		pcpSelector.addListener(this, EventType.EVENT_READ_DATA_AVAILABLE);
//		pcpSelector.addListener(this, EventType.EVENT_CHANNEL_CLOSED);
//	}

	public void setPool(ArrayBlockingQueue<PortalContext> pool) {
		this.pool = pool;
	}

	public void setPCPSelector() {
		setPCPSelector(DemoPCPSelector.getInstance());		
	}

	public void setPCPSelector(PCPSelector pcpSelector) {
//		PCPSelector pcpSelector = null;
//		pcpSelector = DemoPCPSelector.getInstance();
		pcpSelector.addListener(this, EventType.EVENT_READ_DATA_AVAILABLE);
		pcpSelector.addListener(this, EventType.EVENT_CHANNEL_CLOSED);
		LOG.info("DemoPortalContextListener setPCPSelector " + pcpSelector.hashCode());
	}

	// use another thread mechanism to call actual opcodeReceive()
	public void handlePortalContextEvent(PortalContext portalContext, EventType eventType) {
		String str = String.format("thread=%s poolSize=%s pc=%s type=%s", Thread.currentThread().getId(), pool.size(), portalContext.hashCode(), eventType);
		LOG.info("contextEvent enter " + str);

		if (eventType == EventType.EVENT_READ_DATA_AVAILABLE) {
			try {
				FList response = portalContext.opcodeReceive();
				pool.put(portalContext);
//				LOG.info("DemoPortalContextListener return connection. poolSize now " + pool.size());
			} catch (InterruptedException e) {
				LOG.severe("Exception returning context to pool");
			} catch (EBufException e) {
				LOG.severe("DemoPortalContextListener handlePortalContextEvent error");
			}
		} else if (eventType == EventType.EVENT_CHANNEL_CLOSED) {
//			LOG.info("DemoPortalContextListener EVENT_CHANNEL_CLOSED NOOP");
		} else {
			LOG.info("DemoPortalContextListener unhandled event");
		}
	}

}