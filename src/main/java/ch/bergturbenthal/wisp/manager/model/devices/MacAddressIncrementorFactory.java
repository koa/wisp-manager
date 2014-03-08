package ch.bergturbenthal.wisp.manager.model.devices;

import java.util.Iterator;

import lombok.AllArgsConstructor;
import ch.bergturbenthal.wisp.manager.model.MacAddress;

@AllArgsConstructor
public class MacAddressIncrementorFactory {
	private final int addressCount;
	private final long stepSize;

	public Iterable<MacAddress> getAllMacAddresses(final MacAddress baseAddress) {
		return new Iterable<MacAddress>() {

			@Override
			public Iterator<MacAddress> iterator() {

				return new Iterator<MacAddress>() {
					int currentStep = 0;

					@Override
					public boolean hasNext() {
						return (currentStep < addressCount);
					}

					@Override
					public MacAddress next() {
						if (!hasNext()) {
							return null;
						}
						return baseAddress.offsetAddress(currentStep++ * stepSize);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
}
