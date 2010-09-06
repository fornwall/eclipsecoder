package net.fornwall.eclipsecoder.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.Composite;

public class SpacerFieldEditor extends FieldEditor {
	public SpacerFieldEditor(Composite parent) {
		super("", "", parent);
	}

	@Override
	protected void adjustForNumColumns(int numColumns) {
		// do nothing
	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		getLabelControl(parent);
	}

	@Override
	protected void doLoad() {
		// do nothing
	}

	@Override
	protected void doLoadDefault() {
		// do nothing
	}

	@Override
	protected void doStore() {
		// do nothing
	}

	@Override
	public int getNumberOfControls() {
		return 0;
	}
}
