package ch.bergturbenthal.wisp.manager.util;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.data.repository.CrudRepository;

import com.vaadin.data.Container;
import com.vaadin.data.Container.ItemSetChangeNotifier;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.AbstractProperty;

@Slf4j
public abstract class CrudRepositoryContainer<T, ID extends Serializable> implements Container, ItemSetChangeNotifier {

	public static interface PojoFilter<T> {
		boolean accept(final T candidate);
	}

	private final Collection<PojoFilter<T>> filters = new ArrayList<CrudRepositoryContainer.PojoFilter<T>>(1);
	private final List<ItemSetChangeListener> listeners = new ArrayList<Container.ItemSetChangeListener>();
	private final Map<String, PropertyDescriptor> properties = new HashMap<String, PropertyDescriptor>();

	@Setter
	protected CrudRepository<T, ID> repository;

	public CrudRepositoryContainer(final CrudRepository<T, ID> repository, final Class<T> entityType) {
		this.repository = repository;
		for (final PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(entityType)) {
			if (descriptor.getReadMethod() != null) {
				properties.put(descriptor.getName(), descriptor);
			}
		}
	}

	@Override
	public boolean addContainerProperty(final Object propertyId, final Class<?> type, final Object defaultValue) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void addFilter(final PojoFilter<T> filter) {
		filters.add(filter);
	}

	@Override
	public Object addItem() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Item addItem(final Object itemId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addItemSetChangeListener(final ItemSetChangeListener listener) {
		addListener(listener);
	}

	@Override
	public void addListener(final ItemSetChangeListener listener) {
		listeners.add(listener);
	}

	@Override
	public boolean containsId(final Object itemId) {
		return repository.exists((ID) itemId);
	}

	@Override
	public Property getContainerProperty(final Object itemId, final Object propertyId) {
		final PropertyDescriptor propertyDescriptor = properties.get(propertyId);
		if (propertyDescriptor == null) {
			throw new IllegalArgumentException("Property " + propertyId + " not exists");
		}
		final AbstractProperty<Object> property = new AbstractProperty<Object>() {

			@Override
			public Class<? extends Object> getType() {
				return propertyDescriptor.getPropertyType();
			}

			@Override
			public Object getValue() {
				final T foundItem = loadItem(itemId);
				try {
					return propertyDescriptor.getReadMethod().invoke(foundItem);
				} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
					throw new RuntimeException("Cannot read " + propertyId + " from " + foundItem, e);
				}
			}

			@Override
			public void setValue(final Object newValue) throws com.vaadin.data.Property.ReadOnlyException {
				final T foundItem = loadItem(itemId);
				try {
					propertyDescriptor.getWriteMethod().invoke(foundItem, newValue);
				} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
					throw new RuntimeException("Cannot write " + propertyId + " to " + foundItem, e);
				}

			}
		};
		property.setReadOnly(propertyDescriptor.getWriteMethod() == null);
		return property;
	}

	@Override
	public Collection<?> getContainerPropertyIds() {
		return properties.keySet();
	}

	@Override
	public CrudItem<T> getItem(final Object itemId) {
		return new CrudItem<T>() {

			@Override
			public boolean addItemProperty(final Object id, final Property property) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Property getItemProperty(final Object id) {
				return getContainerProperty(itemId, id);
			}

			@Override
			public Collection<?> getItemPropertyIds() {
				return getContainerPropertyIds();
			}

			@Override
			public T getPojo() {
				return loadItem(itemId);
			}

			@Override
			public boolean removeItemProperty(final Object id) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public Collection<ID> getItemIds() {
		final ArrayList<ID> ret = new ArrayList<ID>();
		next_entry:
		for (final T entry : repository.findAll()) {
			for (final PojoFilter<T> filter : filters) {
				if (!filter.accept(entry)) {
					continue next_entry;
				}
			}
			ret.add(idFromValue(entry));
		}
		return ret;
	}

	@Override
	public Class<?> getType(final Object propertyId) {
		return properties.get(propertyId).getPropertyType();
	}

	protected abstract ID idFromValue(final T entry);

	private T loadItem(final Object itemId) {
		return repository.findOne((ID) itemId);
	}

	public void notifyDataChanged() {
		final ItemSetChangeEvent event = new ItemSetChangeEvent() {
			@Override
			public Container getContainer() {
				return CrudRepositoryContainer.this;
			}
		};
		for (final ItemSetChangeListener listener : listeners) {
			listener.containerItemSetChange(event);
		}
	}

	public void removeAllFilters() {
		filters.clear();
	}

	@Override
	public boolean removeAllItems() throws UnsupportedOperationException {
		repository.deleteAll();
		return true;
	}

	@Override
	public boolean removeContainerProperty(final Object propertyId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeItem(final Object itemId) throws UnsupportedOperationException {
		try {
			return repository.exists((ID) itemId);
		} finally {
			repository.delete((ID) itemId);
		}
	}

	@Override
	public void removeItemSetChangeListener(final ItemSetChangeListener listener) {
		removeListener(listener);
	}

	@Override
	public void removeListener(final ItemSetChangeListener listener) {
		listeners.remove(listener);
	}

	@Override
	public int size() {
		return (int) repository.count();
	}

}
