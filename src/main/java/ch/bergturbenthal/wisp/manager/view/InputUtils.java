package ch.bergturbenthal.wisp.manager.view;

import lombok.extern.slf4j.Slf4j;
import ch.bergturbenthal.wisp.manager.util.PojoItem;
import ch.bergturbenthal.wisp.manager.view.component.CustomFieldGroupFactory;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Window;

@Slf4j
public class InputUtils {
	public static interface ResultHandler<T> {
		void onOk(final T value);

		void onCancel();
	}

	public static <T> Window createInputDialog(final PojoItem<T> valueItem, final ResultHandler<T> handler, final String windowTitle, final Object... properties) {
		final Window window = new Window(windowTitle);
		window.setModal(true);
		final FormLayout layout = new FormLayout();
		final FieldGroup addRootRangeFieldGroup = new FieldGroup(valueItem);
		addRootRangeFieldGroup.setFieldFactory(new CustomFieldGroupFactory());
		for (final Object property : properties) {
			layout.addComponent(addRootRangeFieldGroup.buildAndBind(property));
		}

		final Button okButton = new Button("Ok", new Button.ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				try {
					addRootRangeFieldGroup.commit();
					final T inputData = ((PojoItem<T>) addRootRangeFieldGroup.getItemDataSource()).getPojo();
					handler.onOk(inputData);
					window.close();
				} catch (final CommitException e) {
					log.error("Commit error", e);
				}
			}
		});
		final Component cancelButton = new Button("Cancel", new Button.ClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				handler.onCancel();
				window.close();
			}
		});
		layout.addComponent(new HorizontalLayout(cancelButton, okButton));
		window.setContent(layout);
		window.center();
		return window;

	}
}
