package net.fornwall.eclipsecoder.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilderFactory;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Job to launch the TopCoder contest applet. Downloads on demand from
 * http://www.topcoder.com/contest/classes/ContestApplet.jar and delegates actual launching to
 * {@link TopCoderAppletLauncher}.
 */
public class LaunchTopCoderAppletJob extends Job {

	private static final String APPLET_JNLP = "https://community.topcoder.com/contest/arena/ContestAppletProd.jnlp";

	private static final int ESTIMATED_APPLET_SIZE = 2000000;

	private static boolean classLoaderWorks(ClassLoader loader) {
		try {
			loader.loadClass("com.topcoder.client.contestApplet.runner.generic");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static File getLocalAppletJar(URL jarUrl) {
		String jarFileName = jarUrl.getPath().substring(jarUrl.getPath().lastIndexOf('/') + 1);

		File userHome = new File(System.getProperty("user.home"));
		File eclipsecoderDir = new File(userHome, ".eclipsecoder");
		if (((eclipsecoderDir.exists() && eclipsecoderDir.isDirectory()) || eclipsecoderDir.mkdir())
				&& (eclipsecoderDir.canRead() && eclipsecoderDir.canWrite())) {
			// try with global jar location outside workspace
			return new File(eclipsecoderDir, jarFileName);
		}
		IPath stateLocation = EclipseCoderPlugin.getDefault().getStateLocation();
		IPath jarLocation = stateLocation.append(jarFileName);
		return jarLocation.toFile();
	}

	public LaunchTopCoderAppletJob(String name) {
		super(name);
	}

	private IStatus download(IProgressMonitor monitor, URL jarUrl) {
		File jar = getLocalAppletJar(jarUrl);

		try {
			URLConnection connection = jarUrl.openConnection();
			int jarSize = (connection.getContentLength() == -1) ? ESTIMATED_APPLET_SIZE : connection.getContentLength();
			monitor.beginTask("Downloading applet: " + jarUrl, jarSize);

			try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(jar)) {
				byte[] buffer = new byte[2048];
				int i;
				while ((i = in.read(buffer)) != -1) {
					if (monitor.isCanceled()) {
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
		}

		return null;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		URL[] appletJars;

		try {
			monitor.beginTask("Downloading JNLP: " + APPLET_JNLP, IProgressMonitor.UNKNOWN);
			Document jnlpDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(APPLET_JNLP);
			NodeList jarList = jnlpDoc.getElementsByTagName("jar");
			appletJars = new URL[jarList.getLength()];
			for (int i = 0; i < jarList.getLength(); i++) {
				appletJars[i] = new URL(((Element) jarList.item(i)).getAttribute("href"));
			}
		} catch (Exception e) {
			return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK, e.getMessage(), e);
		}

		for (URL appletJar : appletJars) {
			File localAppletJar = getLocalAppletJar(appletJar);
			if (!localAppletJar.exists()) {
				IStatus status = download(monitor, appletJar);
				if (status != null)
					return status;
			}
		}

		for (URL appletJar : appletJars) {
			File localAppletJar = getLocalAppletJar(appletJar);
			if (!localAppletJar.exists()) {
				// should have been downloaded above
				return new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK, "Applet does not exist at "
						+ localAppletJar.getAbsolutePath(), null);
			}
		}

		try {
			for (URL appletJar : appletJars) {
				File localAppletJar = getLocalAppletJar(appletJar);
				long lastModifiedLocal = localAppletJar.lastModified();
				URLConnection connection = appletJar.openConnection();
				long lastModifiedRemote = connection.getLastModified();
				if (lastModifiedRemote > lastModifiedLocal) {
					IStatus status = download(monitor, appletJar);
					if (status != null)
						return status;
				}
			}
			URL[] urls = new URL[appletJars.length + 1];
			for (int i = 0; i < appletJars.length; i++) {
				urls[i] = getLocalAppletJar(appletJars[i]).toURI().toURL();
			}
			Bundle bundle = EclipseCoderPlugin.getDefault().getBundle();
			Path path = new Path("arenaplugin.jar");
			urls[appletJars.length] = FileLocator.find(bundle, path, null);

			ClassLoader parent = Thread.currentThread().getContextClassLoader();
			ClassLoader loader = new URLClassLoader(urls, parent);

			if (classLoaderWorks(loader)) {
				TopCoderAppletLauncher.run(loader);
			} else {
				StartTopCoderAppletAction.getAction().setEnabled(true);
				String message;
				String paths = "";

				boolean flag = true;
				for (URL appletJar : appletJars) {
					File localAppletJar = getLocalAppletJar(appletJar);
					if (!localAppletJar.delete()) {
						paths += "," + localAppletJar.getAbsolutePath();
						flag = false;
					}
				}
				if (flag) {
					message = "The downloaded applet jar could not be used and has been deleted. Try to start again.";
				} else {
					message = "The downloaded applet jar could not be used and cannot be removed - please check the file(s) "
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
