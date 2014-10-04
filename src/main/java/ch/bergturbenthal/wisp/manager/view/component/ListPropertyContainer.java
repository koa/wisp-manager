package ch.bergturbenthal.wisp.manager.view.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import lombok.Setter;

import org.apache.commons.lang.ClassUtils;

import ch.bergturbenthal.wisp.manager.util.PojoItem;
import ch.bergturbenthal.wisp.manager.util.PropertyResolver;
import ch.bergturbenthal.wisp.manager.util.PropertyResolver.PropertyHandler;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.AbstractContainer;
import com.vaadin.data.util.AbstractProperty;

public class ListPropertyContainer<T> extends AbstractContainer implements Container, Container.Indexed, Container.Sortable {

	private final Class<T> beanEntryType;

	@Setter
	private Property<T> dataSourceProperty;

	private final PropertyResolver<T> resolver;

	private boolean[] sortOrders;

	private Object[] sortProperties;

	public ListPropertyContainer(final Class<T> beanEntryType) {
		this.beanEntryType = beanEntryType;
		resolver = new PropertyResolver<T>(beanEntryType);
	}

	@Override
	public boolean addContainerProperty(final Object propertyId, final Class<?> type, final Object defaultValue) throws UnsupportedOperationException {
		final PropertyHandler propertyHandler = resolver.resolveProperty(propertyId);
		return propertyHandler != null;
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
	public Object addItemAfter(final Object previousItemId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Item addItemAfter(final Object previousItemId, final Object newItemId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object addItemAt(final int index) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Item addItemAt(final int index, final Object newItemId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsId(final Object itemId) {
		return ((Integer) itemId).intValue() < size();
	}

	@Override
	public Object firstItemId() {
		return Integer.valueOf(0);
	}

	@Override
	public Property getContainerProperty(final Object itemId, final Object propertyId) {
		return getItem(itemId).getItemProperty(propertyId);
	}

	@Override
	public Collection<?> getContainerPropertyIds() {
		return resolver.knownProperties();
	}

	@Override
	public Object getIdByIndex(final int index) {
		return Integer.valueOf(index);
	}

	@Override
	public PojoItem<T> getItem(final Object itemId) {
		final int itemIndex = ((Integer) itemId).intValue();
		return new PojoItem<T>() {

			@Override
			public boolean addItemProperty(final Object id, final Property property) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Property<Object> getItemProperty(final Object id) {
				return loadContainerProperty(itemIndex, id);
			}

			@Override
			public Collection<?> getItemPropertyIds() {
				return resolver.knownProperties();
			}

			@Override
			public T getPojo() {
				return loadItem(itemIndex);
			}

			@Override
			public boolean removeItemProperty(final Object id) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public Collection<?> getItemIds() {
		final int count = size();

		final ArrayList<Integer> ret = new ArrayList<Integer>(count);
		for (int i = 0; i < count; i++) {
			ret.add(Integer.valueOf(i));
		}
		return ret;
	}

	@Override
	public List<?> getItemIds(final int startIndex, final int numberOfItems) {
		final List<Integer> ret = new ArrayList<Integer>(numberOfItems);
		for (int i = 0; i < numberOfItems; i++) {
			ret.add(Integer.valueOf(i + startIndex));
		}
		return ret;
	}

	@Override
	public Collection<?> getSortableContainerPropertyIds() {
		final ArrayList<String> ret = new ArrayList<String>();
		for (final String property : resolver.knownProperties()) {
			final PropertyHandler resolvedProperty = resolver.resolveProperty(property);
			final Class<?> propertyType = ClassUtils.primitiveToWrapper(resolvedProperty.getPropertyType());
			if (Comparable.class.isAssignableFrom(propertyType)) {
				ret.add(property);
			}
		}
		return ret;
	}

	@Override
	public Class<?> getType(final Object propertyId) {
		return beanEntryType;
	}

	@Override
	public int indexOfId(final Object itemId) {
		return ((Integer) itemId).intValue();
	}

	@Override
	public boolean isFirstId(final Object itemId) {
		return indexOfId(itemId) == 0;
	}

	@Override
	public boolean isLastId(final Object itemId) {
		return itemId.equals(lastItemId());
	}

	@Override
	public Object lastItemId() {
		return Integer.valueOf(size() - 1);
	}

	private Property<Object> loadContainerProperty(final int itemIndex, final Object propertyId) {
		final PropertyHandler propertyDescriptor = resolver.resolveProperty(propertyId);
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
				final T foundItem = loadItem(itemIndex);
				if (foundItem == null) {
					return null;
				}
				return propertyDescriptor.getValue(foundItem);
			}

			@Override
			public void setValue(final Object newValue) throws com.vaadin.data.Property.ReadOnlyException {
				final T foundItem = loadItem(itemIndex);
				propertyDescriptor.setValue(foundItem, newValue);
			}
		};
		property.setReadOnly(!propertyDescriptor.canWrite());
		return property;
	}

	private T loadItem(final int itemIndex) {
		final Object dataValue = dataSourceProperty.getValue();
		if (dataValue == null) {
			return null;
		}
		if (sortOrders != null && sortProperties != null && sortOrders.length == sortProperties.length) {
			final List<T> sortedItemList;
			if (dataValue instanceof Collection) {
				sortedItemList = new ArrayList<T>((Collection<T>) dataValue);
			} else if (dataValue instanceof Iterable) {
				sortedItemList = new ArrayList<T>();
				final Iterator<T> iterator = ((Iterable<T>) dataValue).iterator();
				while (iterator.hasNext()) {
					sortedItemList.add(iterator.next());
				}
			} else {
				throw new IllegalArgumentException(dataValue + " is not Iterable");
			}
			Collections.sort(sortedItemList, new Comparator<T>() {

				@Override
				public int compare(final T o1, final T o2) {
					for (int i = 0; i < sortOrders.length; i++) {
						final PropertyHandler handler = resolver.resolveProperty(sortProperties[i]);
						final Comparable<Object> c1 = (Comparable<Object>) handler.getValue(o1);
						final Comparable<Object> c2 = (Comparable<Object>) handler.getValue(o2);
						final int cmp = c1.compareTo(c2);
						if (cmp != 0) {
							return sortOrders[i] ? cmp : -cmp;
						}
					}
					return 0;
				}
			});
			return sortedItemList.get(itemIndex);
		}
		if (dataValue instanceof List) {
			return ((List<T>) dataValue).get(itemIndex);
		}
		if (dataValue instanceof Iterable) {
			final Iterator<T> iterator = ((Iterable<T>) dataValue).iterator();
			for (int i = 0; i < itemIndex && iterator.hasNext(); i++) {
				iterator.next();
			}
			if (!iterator.hasNext()) {
				throw new IndexOutOfBoundsException("Index " + itemIndex + " is out of bounds");
			}
			return iterator.next();
		}
		if (itemIndex > 0) {
			throw new IndexOutOfBoundsException();
		}
		throw new IllegalArgumentException(dataValue + " is not Iterable");
	}

	@Override
	public Object nextItemId(final Object itemId) {
		if (isLastId(itemId)) {
			return null;
		}
		return Integer.valueOf(indexOfId(itemId) + 1);
	}

	@Override
	public Object prevItemId(final Object itemId) {
		if (isFirstId(itemId)) {
			return null;
		}
		return Integer.valueOf(indexOfId(itemId) - 1);
	}

	@Override
	public boolean removeAllItems() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeContainerProperty(final Object propertyId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeItem(final Object itemId) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		if (dataSourceProperty == null) {
			return 0;
		}
		final Object listValue = dataSourceProperty.getValue();
		if (listValue == null) {
			return 0;
		}
		if (listValue instanceof Collection) {
			return ((Collection) listValue).size();
		}
		return 1;
	}

	@Override
	public void sort(final Object[] propertyId, final boolean[] ascending) {
		this.sortProperties = propertyId;
		this.sortOrders = ascending;
	}
}