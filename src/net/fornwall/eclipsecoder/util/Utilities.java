package net.fornwall.eclipsecoder.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Static utility methods for the EclipseCoder plug-in.
 */
public class Utilities {

	private static class OkCancelDialogRunnable implements Runnable {
		private String message;

		public boolean result;

		private String title;

		public OkCancelDialogRunnable(String title, String message) {
			this.title = title;
			this.message = message;
		}

		public void run() {
			try {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
				Shell shell = workbenchWindow.getShell();
				result = MessageDialog.openQuestion(shell, title, message);
			} catch (Exception e) {
				Utilities.showException(e);
			}
		}
	}

	public static void close(Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] getBytes(String s) {
		try {
			return s.getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			// will never happen as utf-8 is always supported
			throw new RuntimeException(e);
		}
	}

	public static String getFileContents(IFile file) throws CoreException, IOException {
		String lineSeparator = System.getProperty("line.separator");
		BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents(), file.getCharset(true)));
		try {
			StringBuilder stringBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line).append(lineSeparator);
			}
			return stringBuilder.toString();
		} finally {
			reader.close();
		}
	}

	public static String getMatch(String text, String regExp, int group) {
		Matcher matcher = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE).matcher(text);
		return matcher.find() ? matcher.group(group) : null;
	}

	public static String getStackTrace(Exception e) {
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(byteArray, true));
		BufferedReader reader = new BufferedReader(new StringReader(byteArray.toString()));
		String line;
		int count = 0;
		String result = "";
		try {
			while (count++ < 6 && (line = reader.readLine()) != null) {
				result += line + "\n";
			}
		} catch (Exception ee) {
			// should never happen
			ee.printStackTrace();
		}
		return result;
	}

	/**
	 * Get the file from the plugins state location.
	 * 
	 * @param fileName
	 *            The name of the file to get.
	 * @return A file (which does not need to exist) from the plugin state
	 *         location.
	 */
	public static File getStateFile(String fileName) {
		return EclipseCoderPlugin.getDefault().getStateLocation().append(fileName).toFile();
	}

	public static Shell getWindowShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	/**
	 * Run a <code>Runnable</code> in the user interface thread.
	 * 
	 * @param runnable
	 *            the action to run in the user interface thread
	 */
	public static void runInDisplayThread(final Runnable runnable) {
		runInDisplayThread(runnable, false);
	}

	public static void runInDisplayThread(final Runnable runnable, boolean sync) {
		Runnable showExceptionWrapper = new Runnable() {
			public void run() {
				try {
					runnable.run();
				} catch (Exception e) {
					showException(e);
				}
			}
		};

		Display display = PlatformUI.getWorkbench().getDisplay();
		if (Thread.currentThread().equals(display.getThread())) {
			showExceptionWrapper.run();
		} else {
			if (sync) {
				display.syncExec(showExceptionWrapper);
			} else {
				display.asyncExec(showExceptionWrapper);
			}
		}
	}

	public static void setPerspective(final String perspectiveID) {
		runInDisplayThread(new Runnable() {
			public void run() {
				IPerspectiveDescriptor wantedPerspective = PlatformUI.getWorkbench().getPerspectiveRegistry()
						.findPerspectiveWithId(perspectiveID);

				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().setPerspective(wantedPerspective);
			}
		});
	}

	public static void showException(final Exception e) {
		runInDisplayThread(new Runnable() {
			public void run() {
				showExceptionInThread(e);
			}
		});
	}

	static void showExceptionInThread(Exception e) {
		e.printStackTrace();
		IStatus status = new Status(IStatus.ERROR, EclipseCoderPlugin.PLUGIN_ID, IStatus.OK,
				(e.getMessage() == null) ? e.getClass().getName() : e.getMessage(), e);
		EclipseCoderPlugin.getDefault().getLog().log(status);
		ErrorDialog.openError(getWindowShell(), "Exception caught",
				"An unhandled exception was caught.\nThe complete stack trace has been written to the error log.",
				status);
	}

	/**
	 * Display a message dialog with the specified title and message. If
	 * necessary, it does it asynchronously in the GUI thread.
	 * 
	 * @param title
	 *            the title to display for the message box to show
	 * @param message
	 *            the message to display in the message box to show
	 */
	public static void showMessageDialog(final String title, final String message) {
		showMessageDialog(title, message, false);
	}

	public static void showMessageDialog(final String title, final String message, final boolean isError) {
		if (title == null || message == null) {
			throw new IllegalArgumentException("Illegal arguments, title='" + title + "', message='" + message + "'");
		}

		runInDisplayThread(new Runnable() {
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
				Shell shell = workbenchWindow.getShell();
				if (isError) {
					MessageDialog.openError(shell, title, message);
				} else {
					MessageDialog.openInformation(shell, title, message);
				}
			}
		});
	}

	/**
	 * Show an Ok/Cancel dialog (if necessary in the GUI thread) synchronously.
	 * 
	 * @param title
	 *            The title to show for the dialog.
	 * @param message
	 *            The message to show in the dialog.
	 * @return True if the user pressed ok, false if the user pressed cancel.
	 */
	public static boolean showOkCancelDialog(final String title, final String message) {
		OkCancelDialogRunnable runnable = new OkCancelDialogRunnable(title, message);
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.getThread().equals(Thread.currentThread())) {
			runnable.run();
		} else {
			display.syncExec(runnable);
		}
		return runnable.result;
	}

	public static final NullProgressMonitor NULL_MONITOR = new NullProgressMonitor();

	/**
	 * Build a project and launch a Runnable afterwards.
	 * 
	 * See http://www.eclipse.org/articles/Article-Builders/builders.html
	 * 
	 * @param projectToBuild
	 *            the project to build before launching
	 * @param runAfterBuild
	 *            the Runnable to launch after the build on the projectToBuild
	 *            has been completed
	 */
	public static void buildAndRun(final IProject projectToBuild, final Runnable runAfterBuild) {
		(new Thread() {

			@Override
			public void run() {
				try {
					IJobManager manager = Job.getJobManager();
					if (projectToBuild.getWorkspace().isAutoBuilding()) {
						projectToBuild.build(IncrementalProjectBuilder.CLEAN_BUILD, NULL_MONITOR);

						manager.wakeUp(ResourcesPlugin.FAMILY_AUTO_BUILD);
						manager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, NULL_MONITOR);
					} else {
						projectToBuild.build(IncrementalProjectBuilder.FULL_BUILD, NULL_MONITOR);

						manager.wakeUp(ResourcesPlugin.FAMILY_MANUAL_BUILD);
						manager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, NULL_MONITOR);
					}
					runAfterBuild.run();
				} catch (Exception e) {
					Utilities.showException(e);
				}

			}

		}).start();
	}
}