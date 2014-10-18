package ch.bergturbenthal.wisp.manager.view.component;

import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ch.bergturbenthal.wisp.manager.model.Domain;
import ch.bergturbenthal.wisp.manager.model.NetworkDevice;
import ch.bergturbenthal.wisp.manager.util.CrudRepositoryContainer;
import ch.bergturbenthal.wisp.manager.util.PojoItem;

import com.vaadin.data.Container;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;

@Slf4j
@AllArgsConstructor
public class ContainerSelectFieldFactory extends CustomFieldGroupFactory {
	@AllArgsConstructor
	private abstract class ContainerIdConverer<V> implements Converter<Object, V> {
		private final Container container;

		@Override
		public V convertToModel(final Object value, final Class<? extends V> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
			if (value == null) {
				return null;
			}
			return ((PojoItem<V>) container.getItem(value)).getPojo();
		}

		@Override
		public Object convertToPresentation(final V value, final Class<? extends Object> targetType, final Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
			if (value == null) {
				return null;
			}
			return idFromValue(value);
		}

		@Override
		public Class<Object> getPresentationType() {
			return Object.class;
		}

		protected abstract Object idFromValue(final V value);
	}

	private final CrudRepositoryContainer<NetworkDevice, Long> devicesContainer;
	private final CrudRepositoryContainer<Domain, Long> domainContainer;

	@Override
	@SuppressWarnings("rawtypes")
	public <T extends Field> T createField(final Class<?> dataType, final Class<T> fieldType) {
		if (dataType.isAssignableFrom(NetworkDevice.class)) {
			final ComboBox comboBox = new ComboBox("Device", devicesContainer);
			comboBox.setNullSelectionAllowed(true);
			comboBox.setItemCaptionPropertyId("title");
			comboBox.setConverter(new ContainerIdConverer<NetworkDevice>(devicesContainer) {
				@Override
				public Class<NetworkDevice> getModelType() {
					return NetworkDevice.class;
				}

				@Override
				protected Object idFromValue(final NetworkDevice value) {
					return value.getId();
				}

			});
			return (T) comboBox;
		}
		if (dataType.isAssignableFrom(Domain.class)) {
			final ComboBox comboBox = new ComboBox("Domain", domainContainer);
			comboBox.setNullSelectionAllowed(true);
			comboBox.setItemCaptionPropertyId("domainName");
			comboBox.setConverter(new ContainerIdConverer<Domain>(domainContainer) {
				@Override
				public Class<Domain> getModelType() {
					return Domain.class;
				}

				@Override
				protected Object idFromValue(final Domain value) {
					return value.getId();
				}

			});
			return (T) comboBox;
		}
		return super.createField(dataType, fieldType);
	}
}