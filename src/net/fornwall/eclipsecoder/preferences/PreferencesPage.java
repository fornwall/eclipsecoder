package net.fornwall.eclipsecoder.preferences;

import java.util.List;

import net.fornwall.eclipsecoder.languages.LanguageSupportFactory;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that is contributed to the Preferences dialog. By subclassing
 * <samp>FieldEditorPreferencePage</samp>, we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that belongs to the main
 * plug-in class. That way, preferences can be accessed directly via the preference store.
 */
public class PreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencesPage() {
		super(GRID);
		setTitle("EclipseCoder");
		setPreferenceStore(EclipseCoderPlugin.getDefault().getPreferenceStore());
	}

	private StringFieldEditor userNameEditor;

	private StringFieldEditor passwordEditor;

	private BooleanFieldEditor autoLogonEditor;

	@Override
	protected void createFieldEditors() {
		userNameEditor = new StringFieldEditor(EclipseCoderPlugin.PREFERENCE_TC_USERNAME, "TopCoder username",
				getFieldEditorParent());
		addField(userNameEditor);
		passwordEditor = new StringFieldEditor(EclipseCoderPlugin.PREFERENCE_TC_PASSWORD, "TopCoder password",
				getFieldEditorParent());
		passwordEditor.getTextControl(getFieldEditorParent()).setEchoChar((char) 0x25CF);
		addField(passwordEditor);
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		autoLogonEditor = new BooleanFieldEditor(EclipseCoderPlugin.PREFERENCE_TC_AUTOLOGON,
				"Auto-logon when starting the TopCoder applet", getFieldEditorParent());
		addField(autoLogonEditor);
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		List<String> languages = LanguageSupportFactory.supportedLanguages();
		String[][] labelAndValues = new String[languages.size()][2];
		for (int i = 0; i < labelAndValues.length; i++) {
			labelAndValues[i][0] = labelAndValues[i][1] = languages.get(i);
		}
		addField(new RadioGroupFieldEditor(EclipseCoderPlugin.PREFERENCE_LANGUAGE, "Preferred programming language", 1,
				labelAndValues, getFieldEditorParent(), true));
	}

	@Override
	public boolean performOk() {
		if (autoLogonEditor.getBooleanValue()
				&& (userNameEditor.getStringValue().length() == 0 || passwordEditor.getStringValue().length() == 0)) {
			MessageDialog.openError(getShell(), "Incorrect",
					"You need to specify your TopCoder member name and password for auto-logon to work");
			return false;
		}
		return super.performOk();
	}

	public void init(IWorkbench workbench) {
		// from IWorkbenchPreferencePage interface
	}

}