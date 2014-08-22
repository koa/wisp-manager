package ch.bergturbenthal.wisp.manager.util;

import com.vaadin.data.Item;

public interface PojoItem<T> extends Item {
	T getPojo();
}
