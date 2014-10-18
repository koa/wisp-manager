package ch.bergturbenthal.wisp.manager.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import ch.bergturbenthal.wisp.manager.util.PropertyResolver.PropertyHandler;

import com.vaadin.data.Item;
import com.vaadin.data.Property;

public class BeanReferenceItem<T> implements Item, Item.PropertySetChangeNotifier, PojoItem<T> {
	private final Collection<PropertySetChangeListener> changeListeners = new ArrayList<Item.PropertySetChangeListener>();
	private final PropertyResolver<T> resolver;
	private final AtomicReference<T> valueReference = new AtomicReference<T>();

	public BeanReferenceItem(final Class<T> itemType) {
		resolver = new PropertyResolver<T>(itemType);
	}

	public BeanReferenceItem(final T value) {
		this((Class<T>) value.getClass());
		valueReference.set(value);
	}

	@Override
	public boolean addItemProperty(final Object id, final Property property) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addListener(final PropertySetChangeListener listener) {
		changeListeners.add(listener);
	}

	@Override
	public void addPropertySetChangeListener(final PropertySetChangeListener listener) {
		addListener(listener);
	}

	@Override
	public Property getItemProperty(final Object id) {
		final PropertyHandler handler = resolver.resolveProperty(id);
		if (handler == null) {
			throw new IllegalArgumentException("Property " + id + " not found");
		}
		return new Property() {
			private boolean readOnly = !handler.canWrite();

			@Override
			public Class getType() {
				return handler.getPropertyType();
			}

			@Override
			public Object getValue() {
				return handler.getValue(getPojo());
			}

			@Override
			public boolean isReadOnly() {
				return readOnly;
			}

			@Override
			public void setReadOnly(final boolean newStatus) {
				readOnly = newStatus || !handler.canWrite();
			}

			@Override
			public void setValue(final Object newValue) throws ReadOnlyException {
				if (readOnly) {
					throw new ReadOnlyException();
				}
				handler.setValue(getPojo(), newValue);
			}
		};
	}

	@Override
	public Collection<?> getItemPropertyIds() {
		return resolver.knownProperties();
	}

	@Override
	public T getPojo() {
		return valueReference.get();
	}

	@Override
	public boolean removeItemProperty(final Object id) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeListener(final PropertySetChangeListener listener) {
		changeListeners.remove(listener);
	}

	@Override
	public void removePropertySetChangeListener(final PropertySetChangeListener listener) {
		removeListener(listener);
	}

	public void setValue(final T value) {
		valueReference.set(value);
		for (final PropertySetChangeListener listener : changeListeners) {
			listener.itemPropertySetChange(new PropertySetChangeEvent() {
				@Override
				public Item getItem() {
					return BeanReferenceItem.this;
				}
			});
		}
	}

}
