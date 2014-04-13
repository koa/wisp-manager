package ch.bergturbenthal.wisp.manager.view.component;

import ch.bergturbenthal.wisp.manager.util.CrudItem;

public interface ItemEditor<T> {
	void setItem(final CrudItem<T> item);

	CrudItem<T> getCurrentItem();
}
