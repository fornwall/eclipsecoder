package net.fornwall.eclipsecoder.actions;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.util.ReflectUtil;
import net.fornwall.eclipsecoder.util.Utilities;

public class TopCoderAppletLauncher {

	static volatile JFrame tcMainFrame = null;

	private static void addAutoLogon() {
		if (EclipseCoderPlugin.tcAutoLogon()) {
			final String userName = EclipseCoderPlugin.tcUserName();
			final String password = EclipseCoderPlugin.tcPassword();

			Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
				public void eventDispatched(AWTEvent event) {
					if (!(event.getSource().getClass().getName().equals("com.topcoder.client.contestApplet.frames.MainFrame")))
						return;
					if (event.getID() != WindowEvent.WINDOW_ACTIVATED)
						return;

					try {
						logon(event.getSource(), userName, password);
					} catch (Exception e) {
						Utilities.showException(e);
					}

					// remove ourself - just one try should be enough
					Toolkit.getDefaultToolkit().removeAWTEventListener(this);
				}
			}, AWTEvent.WINDOW_EVENT_MASK);
		}
	}

	// remove the listener shutting down the process on window close event
	static void fixShutdown(JFrame mainFrame) {
		for (WindowListener listener : mainFrame.getWindowListeners()) {
			if (listener.getClass().getName().indexOf("contestApplet.runner") != -1) {
				mainFrame.removeWindowListener(listener);
			}
		}
	}

	// fix problem with TopCoder strange verify() method not finding classes
	static void fixVerify(Object mainFrame) {
		Object contestApplet = ReflectUtil.invokeInstanceMethod(mainFrame, "getContestApplet");
		Object contestant = ReflectUtil.invokeInstanceMethod(contestApplet, "getModel");
		ReflectUtil.setInstanceField(contestant, "verified", true);
		ReflectUtil.setInstanceField(contestant, "verifySuccess", true);
	}

	// programmatically fill in credentials and press login button
	static void logon(Object mainFrame, String userName, String userPassword) {
		Object contestApplet = ReflectUtil.invokeInstanceMethod(mainFrame, "getContestApplet");
		Object roomManager = ReflectUtil.invokeInstanceMethod(contestApplet, "getRoomManager");
		Object currentRoom = ReflectUtil.invokeInstanceMethod(roomManager, "getCurrentRoom");

		// work around TopCoder check for classes pressing the login button
		Object contestant = ReflectUtil.invokeInstanceMethod(contestApplet, "getModel");
		String[] validIds = (String[]) ReflectUtil.getField(contestant, "valid");
		String[] newValidIds = new String[validIds.length + 1];
		System.arraycopy(validIds, 0, newValidIds, 0, validIds.length);
		newValidIds[validIds.length] = "net.fornwall";
		ReflectUtil.setInstanceField(contestant, "valid", newValidIds);
		fixVerify(mainFrame);

		JTextField nameInput = (JTextField) ReflectUtil.getInstanceField(
				ReflectUtil.getInstanceField(currentRoom, "userName", true), "component", false);
		JPasswordField passwordInput = (JPasswordField) ReflectUtil.getInstanceField(
				ReflectUtil.getInstanceField(currentRoom, "passWord", true), "component", false);
		final JButton loginButton = (JButton) ReflectUtil.getInstanceField(
				ReflectUtil.getInstanceField(currentRoom, "loginButton", true), "component", false);

		nameInput.setText(userName);
		passwordInput.setText(userPassword);
		// allow the window to repaint itself before pushing button
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					loginButton.doClick();
				} catch (Exception e) {
					Utilities.showException(e);
				}
			}
		});
	}

	public static void run(ClassLoader loader) {
		StartTopCoderAppletAction.getAction().setEnabled(false);
		addAutoLogon();
		if (tcMainFrame != null) {
			tcMainFrame.setVisible(true);
		} else {
			startContestAppletFirstTime(loader);
		}
	}

	private static File getContestAppletConfDir() {
		File userHome = new File(System.getProperty("user.home"));
		File eclipsecoderDir = new File(userHome, ".eclipsecoder");
		if (((eclipsecoderDir.exists() && eclipsecoderDir.isDirectory()) || eclipsecoderDir.mkdir())
				&& (eclipsecoderDir.canRead() && eclipsecoderDir.canWrite())) {
			// try with contestapplet.conf location outside workspace
			return eclipsecoderDir;
		}
		return EclipseCoderPlugin.getDefault().getStateLocation().toFile();
	}

	@SuppressWarnings("unchecked")
	private static void startContestAppletFirstTime(final ClassLoader loader) {
		// do not overwrite existing contestapplet.conf in home:
		System.setProperty("com.topcoder.client.contestApplet.common.LocalPreferences.filelocation",
				getContestAppletConfDir().getAbsolutePath());

		try {
			Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
				public void eventDispatched(AWTEvent event) {
					try {
						if (!(event.getSource().getClass().getName().equals("com.topcoder.client.contestApplet.frames.MainFrame")))
							return;
						if (event.getID() != WindowEvent.WINDOW_ACTIVATED)
							return;

						final JFrame mainFrame = (JFrame) event.getSource();

						fixVerify(mainFrame);
						fixShutdown(mainFrame);

						mainFrame.addWindowListener(new WindowAdapter() {
							@Override
							public void windowClosed(WindowEvent e) {
								tcMainFrame = mainFrame;
								StartTopCoderAppletAction.getAction().setEnabled(true);
							}
						});

						// remove ourself - just one try should be
						// enough
						Toolkit.getDefaultToolkit().removeAWTEventListener(this);

						Class<?> eclipseCoderEntryPointClass = loader
								.loadClass("net.fornwall.eclipsecoder.arena.EclipseCoderEntryPoint");

						Class<?> pluginManagerClass = loader
								.loadClass("com.topcoder.client.contestApplet.editors.PluginManager");
						Method getInstanceMethod = pluginManagerClass.getMethod("getInstance");
						Object pluginManager = getInstanceMethod.invoke(null);
						// PluginManager pluginManager =
						// PluginManager.getInstance();

						Class<?> editorPluginClass = loader
								.loadClass("com.topcoder.client.contestApplet.editors.EditorPlugin");
						Constructor<?> constructorMethod = editorPluginClass.getConstructor(String.class, String.class,
								String.class, boolean.class);
						Object editorPlugin = constructorMethod.newInstance("EclipseCoder",
								eclipseCoderEntryPointClass.getName(), "", false);
						// EditorPlugin editorPlugin = new
						// EditorPlugin("EclipseCoder",
						// EclipseCoderEntryPoint.class.getName(), "",
						// false);

						Method setEagerMethod = editorPlugin.getClass().getMethod("setEager", boolean.class);
						setEagerMethod.invoke(editorPlugin, true);
						// editorPlugin.setEager(true);

						String classPath = StartTopCoderAppletAction.class.getProtectionDomain().getCodeSource()
								.getLocation().getPath();

						Method setClassPathMethod = editorPlugin.getClass().getMethod("setClassPath", String.class);
						setClassPathMethod.invoke(editorPlugin, classPath);
						// editorPlugin.setClassPath(classPath);

						Object editorPluginsArray = Array.newInstance(editorPluginClass, 1);
						Array.set(editorPluginsArray, 0, editorPlugin);
						Method setEditorPluginsMethod = pluginManager.getClass().getMethod("setEditorPlugins",
								editorPluginsArray.getClass());
						setEditorPluginsMethod.invoke(pluginManager, editorPluginsArray);
						// pluginManager.setEditorPlugins(new
						// EditorPlugin[] {
						// editorPlugin
						// });

						Class<?> localPreferencesClass = loader
								.loadClass("com.topcoder.client.contestApplet.common.LocalPreferences");
						getInstanceMethod = localPreferencesClass.getMethod("getInstance");
						Object localPreferencesInstance = getInstanceMethod.invoke(null);
						Method setDefaultEditorNameMethod = localPreferencesInstance.getClass().getMethod(
								"setDefaultEditorName", String.class);
						setDefaultEditorNameMethod.invoke(localPreferencesInstance, "EclipseCoder");
						// LocalPreferences.getInstance().setDefaultEditorName("EclipseCoder");

						Field cacheField = pluginManagerClass.getDeclaredField("cache");
						cacheField.setAccessible(true);
						Map<String, Object> pluginsCache = (Map<String, Object>) cacheField.get(pluginManager);

						Class<?> dynamicEditorClass = loader
								.loadClass("com.topcoder.client.contestApplet.editors.DynamicEditor");
						Constructor<?> dynamicEditorConstructor = dynamicEditorClass.getConstructor(editorPluginClass);
						Object ownEditor = dynamicEditorConstructor.newInstance(editorPlugin);
						// DynamicEditor ownEditor = new
						// DynamicEditor(editorPlugin);
						pluginsCache.put("EclipseCoder", ownEditor);
						Field editorField = dynamicEditorClass.getDeclaredField("editor");
						editorField.setAccessible(true);

						Object eclipseCoderEntryPoint = eclipseCoderEntryPointClass.newInstance();
						editorField.set(ownEditor, eclipseCoderEntryPoint);

						// the methodCache is built in constructor
						// against custom
						// loaded
						// class and needs to be
						// replaced
						Field methodCacheField = dynamicEditorClass.getDeclaredField("methodCache");
						methodCacheField.setAccessible(true);
						Map<String, Method> methodCache = (Map<String, Method>) methodCacheField.get(ownEditor);
						for (Method m : eclipseCoderEntryPointClass.getMethods()) {
							if (m.getName().equals("loadClasses")) {
								m.invoke(eclipseCoderEntryPoint);
							}
							methodCache.put(m.getName(), m);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, AWTEvent.WINDOW_EVENT_MASK);

			Class<?> genericClass = loader.loadClass("com.topcoder.client.contestApplet.runner.generic");
			Method mainMethod = genericClass.getMethod("main", String[].class);
			// arguments taken from the JNLP file:
			// http://www.topcoder.com/contest/arena/ContestAppletProd.jnlp
			mainMethod.invoke(null, new Object[] { new String[] { "www.topcoder.com", "5001",
					"http://tunnel1.topcoder.com/dummy?tunnel", "TopCoder" } });
		} catch (Exception e) {
			Utilities.showException(e);
			StartTopCoderAppletAction.getAction().setEnabled(true);
		}
	}
}
