package com.shatter.demo;

import java.io.IOException;
import java.util.logging.Logger;

import com.portal.pcm.PCPSelector;

public class DemoPCPSelector extends PCPSelector implements Runnable {
	static final Logger LOG = Logger.getLogger("DemoPCPSelector");
	private static DemoPCPSelector instance = null;

	private DemoPCPSelector() throws IOException {
		super();
	}

	public static synchronized DemoPCPSelector getInstance() throws RuntimeException {
		if (instance == null) {
			try {
				instance = new DemoPCPSelector();
			} catch (IOException e) {
				LOG.severe("Error creating DemoPCPSelector");
				throw new RuntimeException(e);
			}
		}
		return instance;
	}

	public void run() {
		LOG.info(this + "run enter thread=" + Thread.currentThread().getId());
		process();
		LOG.info(this + "run return thread=" + Thread.currentThread().getId());
	}
}
