package ch.bergturbenthal.wisp.manager.view.component;

import ch.bergturbenthal.wisp.manager.util.PojoItem;

public interface ItemEditor<T> {
	void setItem(final PojoItem<T> item);

	PojoItem<T> getCurrentItem();
}
