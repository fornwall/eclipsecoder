package net.fornwall.eclipsecoder.actions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;

import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

/**
 * Job to launch the TopCoder contest applet. Downloads on demand from
 * http://www.topcoder.com/contest/classes/ContestApplet.jar and delegates actual launching to
 * {@link TopCoderAppletLauncher}.
 */
public class LaunchTopCoderAppletJob extends Job {

	private static final String APPLET_JNLP = "http://community.topcoder.com/contest/arena/ContestAppletProd.jnlp";
	private static final String JAR_REGEX = "<jar\\s*href=\"(.*?)\"\\s*/>";
	private static String[][] APPLET_JARS;

	private static final int ESTIMATED_APPLET_SIZE = 2000000;

	private static boolean classLoaderWorks(ClassLoader loader) {
		try {
			loader.loadClass("com.topcoder.client.contestApplet.runner.generic");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static File getLocalAppletJar(String[] jarname) {
		File userHome = new File(System.getProperty("user.home"));
		File eclipsecoderDir = new File(userHome, ".eclipsecoder");
		if (((eclipsecoderDir.exists() && eclipsecoderDir.isDirectory()) || eclipsecoderDir.mkdir())
				&& (eclipsecoderDir.canRead() && eclipsecoderDir.canWrite())) {
			// try with global jar location outside workspace
			return new File(eclipsecoderDir, jarname[1]);
		}
		IPath stateLocation = EclipseCoderPlugin.getDefault().getStateLocation();
		IPath jarLocation = stateLocation.append(jarname[1]);
		return jarLocation.toFile();
	}

	public LaunchTopCoderAppletJob(String name) {
		super(name);
	}

	private IStatus download(IProgressMonitor monitor, String[] jarname) {
		File jar = getLocalAppletJar(jarname);
		InputStream in = null;
		OutputStream out = null;

		try {
			URLConnection connection = new URL(jarname[0]).openConnection();
			int jarSize = (connection.getContentLength() == -1) ? ESTIMATED_APPLET_SIZE : connection.getContentLength();
			monitor.beginTask("Downloading applet: " + jarname[1], jarSize);

			in = connection.getInputStream();
			out = new FileOutputStream(jar);
			byte[] buffer = new byte[2048];
			int i;
			while ((i = in.read(buffer)) != -1) {
				if (monitor.isCanceled()) {
					Utilities.close(out);
					if (jar.exists()) {
						if (!jar.delete()) {
							Utilities.showMessageDialog("Cannot delete file",
									"Cannot delete applet file:\n" + jar.getAbsolutePath()
											+ "\n\nCheck permissions if problem persists.", true);
						}
					}
					StartTopCoderAppletAction.getAction().setEnabled(true);
					return Status.CANCEL_STATUS;
				}
				out.write(buffer, 0, i);
				monitor.worked(i);
			}
		} catch (MalformedURLException e) {
			// should never happen as APPLET_URL is a valid URL
			if (jar.exists()) {
				jar.delete();
			}
			StartTopCoderAppletAction.getAction().setEnabled(true);
			Utilities.showException(e);
		} catch (IOException e) {
			if (jar.exists()) {
				jar.delete();
			}
			StartTopCoderAppletAction.getAction().setEnabled(true);
			return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK, e.getMessage(), e);
		} finally {
			Utilities.close(in);
			Utilities.close(out);
		}

		return null;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		BufferedReader br = null;
		try {
			URLConnection connection = new URL(APPLET_JNLP).openConnection();
			br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			ArrayList<String> jarList = new ArrayList<String>();
			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (line.matches(JAR_REGEX))
					jarList.add(line.replaceAll(JAR_REGEX, "$1"));
				line = br.readLine();
			}
			APPLET_JARS = new String[jarList.size()][2];
			for (int i = 0; i < jarList.size(); i++) {
				APPLET_JARS[i][0] = jarList.get(i);
				APPLET_JARS[i][1] = jarList.get(i).substring(jarList.get(i).lastIndexOf('/'));
				APPLET_JARS[i][1] = APPLET_JARS[i][1].substring(1);
			}
		} catch (Exception e) {
			return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK, e.getMessage(), e);
		} finally {
			Utilities.close(br);
		}

		for (String[] jarname : APPLET_JARS) {
			File localAppletJar = getLocalAppletJar(jarname);
			if (!localAppletJar.exists()) {
				IStatus status = download(monitor, jarname);
				if (status != null)
					return status;
			}
		}

		for (String[] jarname : APPLET_JARS) {
			File localAppletJar = getLocalAppletJar(jarname);
			if (!localAppletJar.exists()) {
				// should have been downloaded above
				return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK, "Applet does not exist at "
						+ localAppletJar.getAbsolutePath(), null);
			}
		}

		try {
			for (String[] jarname : APPLET_JARS) {
				File localAppletJar = getLocalAppletJar(jarname);
				long lastModifiedLocal = localAppletJar.lastModified();
				URLConnection connection = new URL(jarname[0]).openConnection();
				long lastModifiedRemote = connection.getLastModified();
				if (lastModifiedRemote > lastModifiedLocal) {
					IStatus status = download(monitor, jarname);
					if (status != null)
						return status;
				}
			}
			URL[] urls = new URL[APPLET_JARS.length + 1];
			for (int i = 0; i < APPLET_JARS.length; i++) {
				urls[i] = getLocalAppletJar(APPLET_JARS[i]).toURI().toURL();
			}
			Bundle bundle = EclipseCoderPlugin.getDefault().getBundle();
			Path path = new Path("arenaplugin.jar");
			urls[APPLET_JARS.length] = FileLocator.find(bundle, path, null);

			ClassLoader parent = Thread.currentThread().getContextClassLoader();
			ClassLoader loader = new URLClassLoader(urls, parent);

			if (classLoaderWorks(loader)) {
				TopCoderAppletLauncher.run(loader);
			} else {
				StartTopCoderAppletAction.getAction().setEnabled(true);
				String message;
				String paths = "";

				boolean flag = true;
				for (String[] jarname : APPLET_JARS) {
					File localAppletJar = getLocalAppletJar(jarname);
					if (!localAppletJar.delete()) {
						paths += "," + localAppletJar.getAbsolutePath();
						flag = false;
					}
				}
				if (flag) {
					message = "The downloaded applet jar could not be used and has been deleted. Try to start again.";
					// Utilities
					// .showMessageDialog(
					// "Error starting applet",
					// "The downloaded applet jar could not be used and has
					// been deleted. Try to start again.",
					// true);
				} else {
					// Utilities.showMessageDialog("Error starting applet",
					// "The downloaded applet jar could not be used and cannot
					// be removed - please check the file "
					// + getLocalAppletJar().getAbsolutePath(), true);
					message = "The downloaded applet jar could not be used and cannot be removed - please check the file"
							+ paths;
				}
				return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK, message, null);
			}

		} catch (Exception e) {
			return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK, e.getMessage(), e);
		}

		return Status.OK_STATUS;
	}
}
