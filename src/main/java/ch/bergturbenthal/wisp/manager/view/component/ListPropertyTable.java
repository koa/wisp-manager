package ch.bergturbenthal.wisp.manager.view.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Table;

public class ListPropertyTable<T> extends Table {

	private BeanItemContainer<T> connectionDataSource;

	public ListPropertyTable(final Class<T> type) {
		super();
		initType(type);
	}

	public ListPropertyTable(final Class<T> type, final String caption) {
		super(caption);
		initType(type);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<Collection<?>> getType() {
		return (Class<Collection<?>>) (Class<?>) Collection.class;
	}

	private void initType(final Class<T> type) {
		connectionDataSource = new BeanItemContainer<>(type);
		setContainerDataSource(connectionDataSource);
	}

	private List<T> makeList(final Object value) {
		if (value instanceof List) {
			return (List<T>) value;
		}
		if (value instanceof Collection) {
			return new ArrayList<>((Collection) value);
		}
		return (List<T>) Collections.singletonList(value);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void setPropertyDataSource(final Property newDataSource) {
		@SuppressWarnings("unchecked")
		final List<T> value = makeList(newDataSource.getValue());
		connectionDataSource.removeAllItems();
		if (value != null) {
			for (final T v : value) {
				connectionDataSource.addBean(v);
			}
		}
		super.setPropertyDataSource(newDataSource);
	}
}
