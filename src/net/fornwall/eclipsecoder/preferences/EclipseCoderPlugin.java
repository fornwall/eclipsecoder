package net.fornwall.eclipsecoder.preferences;

import java.util.List;

import net.fornwall.eclipsecoder.languages.LanguageSupportFactory;
import net.fornwall.eclipsecoder.stats.ProblemStatement;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class EclipseCoderPlugin extends AbstractUIPlugin {

	private static EclipseCoderPlugin instance;

	public static final String PLUGIN_ID = "net.fornwall.eclipsecoder";

	public static final String PREFERENCE_LANGUAGE = "languagePreference";

	/**
	 * Boolean specifying if user should be automatically logged in when starting TopCoder arena applet.
	 */
	public static final String PREFERENCE_TC_AUTOLOGON = "tcAutoLogon";

	public static final String PREFERENCE_TC_PASSWORD = "tcPassword";

	public static final String PREFERENCE_TC_USERNAME = "tcUserName";

	/**
	 * Show a message to the user specifying that a TopCoder member account is needed. Follow this by opening the
	 * preferences page allowing him to enter information about this account.
	 */
	public static void demandTcAccountSpecified() {
		Utilities.runInDisplayThread(new Runnable() {
			public void run() {
				Utilities.showMessageDialog("TopCoder account needed",
						"You need to specify your TopCoder account credentials.");
				IPreferencePage page = new PreferencesPage();
				PreferenceManager mgr = new PreferenceManager();
				IPreferenceNode node = new PreferenceNode(PreferencesPage.class.getCanonicalName(), page);
				mgr.addToRoot(node);
				PreferenceDialog dialog = new PreferenceDialog(Utilities.getWindowShell(), mgr);
				dialog.create();
				dialog.setMessage(page.getTitle());
				dialog.open();
			}

		});
	}

	public static EclipseCoderPlugin getDefault() {
		return instance;
	}

	public static ProblemStatement getProblemStatement(IProject project) {
		IEclipsePreferences prefs = getProjectPrefs(project);
		if (prefs == null)
			return null;
		String xmlString = prefs.get(ProblemStatement.XML_PREFS_KEY, null);
		if (xmlString == null)
			return null;
		try {
			return ProblemStatement.fromXML(xmlString);
		} catch (Exception e) {
			Utilities.showException(e);
			return null;
		}
	}

	public static IEclipsePreferences getProjectPrefs(IProject project) {
		IScopeContext context = new ProjectScope(project);
		IEclipsePreferences prefs = context.getNode(EclipseCoderPlugin.PLUGIN_ID);
		return prefs;
	}

	public static boolean isTcAccountSpecified() {
		String user = tcUserName();
		String pass = tcPassword();
		return user != null && !(user.length() == 0) && pass != null && !(pass.length() == 0);
	}

	public static String preferedLanguage() {
		List<String> supported = LanguageSupportFactory.supportedLanguages();

		String preferred = instance.getPreferenceStore().getString(EclipseCoderPlugin.PREFERENCE_LANGUAGE);
		if (preferred == null) {
			return supported.get(0);
		}
		// make sure preferred is still supported:
		for (String s : supported) {
			if (s.equals(preferred))
				return preferred;
		}
		return supported.get(0);
	}

	public static boolean tcAutoLogon() {
		return instance.getPreferenceStore().getBoolean(EclipseCoderPlugin.PREFERENCE_TC_AUTOLOGON);
	}

	public static String tcPassword() {
		return instance.getPreferenceStore().getString(EclipseCoderPlugin.PREFERENCE_TC_PASSWORD);
	}

	public static String tcUserName() {
		return instance.getPreferenceStore().getString(EclipseCoderPlugin.PREFERENCE_TC_USERNAME);
	}

	public EclipseCoderPlugin() {
		EclipseCoderPlugin.instance = this;
	}
}
