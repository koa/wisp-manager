package ch.bergturbenthal.wisp.manager.model.test;

import org.junit.Assert;
import org.junit.Test;

import ch.bergturbenthal.wisp.manager.model.MacAddress;

public class MacAddressTest {
	@Test
	public void testConvert() {
		Assert.assertEquals("80:ee:73:67:df:16", new MacAddress("80:ee:73:67:df:16").getAddress());
		Assert.assertEquals("80:ee:73:67:df:16", new MacAddress("80:ee:73:67:DF:16").getAddress());
		Assert.assertEquals("80:0e:73:67:df:16", new MacAddress("80:0e:73:67:df:16").getAddress());
		Assert.assertEquals("80:0e:73:67:df:16", new MacAddress("80:e:73:67:df:16").getAddress());
		Assert.assertEquals("80:ee:73:67:df:16", new MacAddress("80-ee-73-67-df-16").getAddress());
		Assert.assertEquals("80:ee:73:67:df:16", new MacAddress("80ee7367df16").getAddress());
		Assert.assertEquals("80:ee:73:67:df:16", new MacAddress("80EE7367DF16").getAddress());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalAddress() {
		new MacAddress("Hello World");
	}

	@Test
	public void testNextAddress() {
		Assert.assertEquals("80:ee:73:67:df:17", new MacAddress("80:ee:73:67:df:16").nextAddress().getAddress());
	}
}
