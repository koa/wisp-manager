package ch.bergturbenthal.wisp.manager.view.component;

import java.util.Collection;

import lombok.extern.slf4j.Slf4j;

import com.vaadin.data.Property;
import com.vaadin.ui.Table;

@Slf4j
public class ListPropertyTable<T> extends Table {

	private ListPropertyContainer<T> containerDataSource;

	public ListPropertyTable(final Class<T> type) {
		super();
		initType(type);
	}

	public ListPropertyTable(final Class<T> type, final String caption) {
		super(caption);
		initType(type);
	}

	public ListPropertyTable(final ListPropertyContainer<T> containerDataSource) {
		super();
		this.containerDataSource = containerDataSource;
		setContainerDataSource(containerDataSource);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<Collection<?>> getType() {
		return (Class<Collection<?>>) (Class<?>) Collection.class;
	}

	private void initType(final Class<T> type) {
		containerDataSource = new ListPropertyContainer<T>(type);
		setContainerDataSource(containerDataSource);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setPropertyDataSource(final Property newDataSource) {
		containerDataSource.setDataSourceProperty(newDataSource);
		super.setPropertyDataSource(newDataSource);
	}
}
