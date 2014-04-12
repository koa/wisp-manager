package ch.bergturbenthal.wisp.manager.util;

import com.vaadin.data.Item;

public interface CrudItem<T> extends Item {
	T getPojo();
}
