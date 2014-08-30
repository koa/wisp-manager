package ch.bergturbenthal.wisp.manager.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeanUtils;

public class PropertyResolver<T> {
	public static interface PropertyHandler {
		Class<?> getPropertyType();

		void setValue(final Object bean, final Object value);

		Object getValue(final Object bean);

		boolean canWrite();
	}

	private final Map<String, PropertyHandler> properties = new HashMap<String, PropertyHandler>();

	public PropertyResolver(final Class<T> entityType) {
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

	public Collection<String> knownProperties() {
		return properties.keySet();
	}

	public PropertyHandler resolveProperty(final Object propertyId) {
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
}
