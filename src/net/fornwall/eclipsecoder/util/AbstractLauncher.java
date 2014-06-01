package net.fornwall.eclipsecoder.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;

/**
 * See http://www.eclipse.org/articles/Article-Launch-Framework/launch.html
 */
public abstract class AbstractLauncher implements Runnable {

	private ILaunchConfigurationWorkingCopy createLaunchWorkingCopy() throws CoreException {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = manager.getLaunchConfigurationType(getLauncherTypeId());
		String launchConfigName = manager.generateLaunchConfigurationName(getLauncherName());
		return configType.newInstance(null, launchConfigName);
	}

	/**
	 * Return launch configuration name that is seen in the user interface in the Run&Debug dialogs.
	 */
	protected abstract String getLauncherName();

	/**
	 * Return the id for a launch configuration type extension.
	 * 
	 * @see ILaunchManager#getLaunchConfigurationType(String)
	 */
	protected abstract String getLauncherTypeId();

	public final void launch() {
		try {
			ILaunchConfigurationWorkingCopy workingCopy = createLaunchWorkingCopy();
			setUpConfiguration(workingCopy);
			final ILaunchConfiguration configuration = workingCopy.doSave();
			Utilities.runInDisplayThread(new Runnable() {

				public void run() {
					// must be called from ui thread
					DebugUITools.launch(configuration, ILaunchManager.RUN_MODE);
				}

			});
		} catch (Exception e) {
			Utilities.showException(e);
		}
	}

	/** Adapt to Runnable interface. */
	public final void run() {
		launch();
	}

	protected abstract void setUpConfiguration(ILaunchConfigurationWorkingCopy config) throws Exception;
}
