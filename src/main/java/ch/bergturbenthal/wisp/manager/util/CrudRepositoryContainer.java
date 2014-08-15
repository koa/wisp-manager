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

	private static interface PropertyHandler {
		Class<?> getPropertyType();

		void setValue(final Object bean, final Object value);

		Object getValue(final Object bean);

		boolean canWrite();
	}

	private final Collection<PojoFilter<T>> filters = new ArrayList<CrudRepositoryContainer.PojoFilter<T>>(1);
	private final List<ItemSetChangeListener> listeners = new ArrayList<Container.ItemSetChangeListener>();
	private final Map<String, PropertyHandler> properties = new HashMap<String, PropertyHandler>();

	@Setter
	protected CrudRepository<T, ID> repository;

	public CrudRepositoryContainer(final CrudRepository<T, ID> repository, final Class<T> entityType) {
		this.repository = repository;
		final PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(entityType);
		for (final PropertyDescriptor descriptor : propertyDescriptors) {
			if (descriptor.getReadMethod() != null) {
				properties.put(descriptor.getName(), new PropertyHandler() {

					@Override
					public boolean canWrite() {
						return descriptor.getWriteMethod() != null;
					}

					@Override
					public Class<?> getPropertyType() {
						return descriptor.getPropertyType();
					}

					@Override
					public Object getValue(final Object bean) {
						if (bean == null) {
							return null;
						}
						try {
							return descriptor.getReadMethod().invoke(bean);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new RuntimeException("Cannot read property " + descriptor.getDisplayName() + " from " + bean, e);
						}
					}

					@Override
					public void setValue(final Object bean, final Object value) {
						if (bean == null) {
							return;
						}
						try {
							descriptor.getWriteMethod().invoke(bean, value);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new RuntimeException("Cannot write property " + descriptor.getDisplayName() + " with value " + value + " to " + bean, e);
						}
					}

				});
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

	protected Property getContainerDummyProperty(final Object propertyId) {
		final PropertyHandler propertyDescriptor = resolveProperty(propertyId);
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
				return null;
			}

			@Override
			public void setValue(final Object newValue) throws com.vaadin.data.Property.ReadOnlyException {
				throw new ReadOnlyException("Cannot setValue on dummy property");
			}
		};
		// property.setReadOnly(propertyDescriptor.getWriteMethod() == null);
		property.setReadOnly(true);
		return property;
	}

	@Override
	public Property getContainerProperty(final Object itemId, final Object propertyId) {
		final PropertyHandler propertyDescriptor = resolveProperty(propertyId);
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
				if (foundItem == null) {
					return null;
				}
				return propertyDescriptor.getValue(foundItem);
			}

			@Override
			public void setValue(final Object newValue) throws com.vaadin.data.Property.ReadOnlyException {
				final T foundItem = loadItem(itemId);
				propertyDescriptor.setValue(foundItem, newValue);
			}
		};
		property.setReadOnly(!propertyDescriptor.canWrite());
		return property;
	}

	@Override
	public Collection<?> getContainerPropertyIds() {
		return properties.keySet();
	}

	public CrudItem<T> getDummyItem() {
		return new CrudItem<T>() {

			@Override
			public boolean addItemProperty(final Object id, final Property property) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Property getItemProperty(final Object id) {
				return getContainerDummyProperty(id);
			}

			@Override
			public Collection<?> getItemPropertyIds() {
				return getContainerPropertyIds();
			}

			@Override
			public T getPojo() {
				return null;
			}

			@Override
			public boolean removeItemProperty(final Object id) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}
		};
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
		return resolveProperty(propertyId).getPropertyType();
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

	private PropertyHandler resolveProperty(final Object propertyId) {
		final String valueName = String.valueOf(propertyId);
		if (properties.containsKey(valueName)) {
			return properties.get(valueName);
		}
		final int splitPoint = valueName.lastIndexOf('.');
		if (splitPoint < 0) {
			return null;
		}
		final String beforePt = valueName.substring(0, splitPoint);
		final PropertyHandler baseProperty = resolveProperty(beforePt);
		if (baseProperty == null) {
			return null;
		}
		final PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(baseProperty.getPropertyType());
		for (final PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			if (propertyDescriptor.getReadMethod() != null) {
				final String subPropertyName = propertyDescriptor.getName();
				properties.put(beforePt + "." + subPropertyName, new PropertyHandler() {

					@Override
					public boolean canWrite() {
						return propertyDescriptor.getWriteMethod() != null;
					}

					@Override
					public Class<?> getPropertyType() {
						return propertyDescriptor.getPropertyType();
					}

					@Override
					public Object getValue(final Object bean) {
						final Object baseValue = baseProperty.getValue(bean);
						if (baseValue == null) {
							return null;
						}
						try {
							return propertyDescriptor.getReadMethod().invoke(baseValue);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new RuntimeException("Cannot read property " + propertyDescriptor.getDisplayName() + " from " + baseValue, e);
						}
					}

					@Override
					public void setValue(final Object bean, final Object value) {
						final Object baseObject = baseProperty.getValue(bean);
						if (baseObject == null) {
							throw new com.vaadin.data.Property.ReadOnlyException("cannot set property of null object");
						}
						try {
							propertyDescriptor.getWriteMethod().invoke(baseObject, value);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new RuntimeException("Cannot write property " + propertyDescriptor.getDisplayName() + " with value " + value + " to " + baseObject, e);
						}
					}
				});
			}
		}
		return properties.get(valueName);
	}

	@Override
	public int size() {
		return (int) repository.count();
	}

}
