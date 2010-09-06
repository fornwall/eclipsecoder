package net.fornwall.eclipsecoder.languages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

public class LanguageSupportFactory {

	private static final String LANGUAGE_ATTRIBUTE_NAME = "language";

	public static final String LANGUAGE_EXTENSION_SUPPORT_ID = "net.fornwall.eclipsecoder.languagesupport";

	/**
	 * Create a <code>LanguageSupport</code> instance for the given
	 * programming language.
	 * 
	 * @param languageName
	 *            The name of the programming language.
	 * @return a language support if one is found, or null if no one could be
	 *         found
	 */
	public static LanguageSupport createLanguageSupport(String languageName) throws Exception {
		for (IConfigurationElement element : getLanguageExtensions()) {
			String supportedLanguage = element.getAttribute(LANGUAGE_ATTRIBUTE_NAME);
			if (supportedLanguage.equals(languageName)) {
				LanguageSupport result = (LanguageSupport) element.createExecutableExtension("class");
				return result;
			}
		}

		return null;
	}

	private static IConfigurationElement[] getLanguageExtensions() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(LANGUAGE_EXTENSION_SUPPORT_ID);
		return extensionPoint.getConfigurationElements();
	}

	/**
	 * Get all supported languages (where support for a programming language is
	 * given by a plug-in). The names of the returned languages can be used to
	 * create a <code>LanguageSupport</code> instance using
	 * {@link #createLanguageSupport(String)}.
	 * 
	 * @return a sorted list with names of all supported languages
	 */
	public static List<String> supportedLanguages() {
		List<String> result = new ArrayList<String>();
		for (IConfigurationElement element : getLanguageExtensions()) {
			String supportedLanguage = element.getAttribute(LANGUAGE_ATTRIBUTE_NAME);
			result.add(supportedLanguage);
		}
		Collections.sort(result);
		return result;
	}

	private LanguageSupportFactory() {
		// never called - only static methods used
	}
}
