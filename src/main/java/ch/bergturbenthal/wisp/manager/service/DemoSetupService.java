package ch.bergturbenthal.wisp.manager.service;

import ch.bergturbenthal.wisp.manager.model.Antenna;
import ch.bergturbenthal.wisp.manager.model.Station;

public interface DemoSetupService {

	void initDemoData();

	void fillDummyDevice(final Station station);

	void fillDummyDevice(final Antenna antenna);

}