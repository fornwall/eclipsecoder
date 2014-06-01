package net.fornwall.eclipsecoder.actions;

import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * The action for starting the TopCoder contest applet. Just launches as {@link LaunchTopCoderAppletJob}.
 */
public class StartTopCoderAppletAction extends Action {

	public StartTopCoderAppletAction() {
		super("Start", ImageDescriptor.createFromURL(Platform.getBundle(EclipseCoderPlugin.PLUGIN_ID).getEntry(
				"icons/tc_logo.gif")));
		setToolTipText("Start the TopCoder contest applet");
		instance = this;
	}

	public static StartTopCoderAppletAction getAction() {
		return instance;
	}

	private static StartTopCoderAppletAction instance;

	@Override
	public void run() {
		setEnabled(false);
		LaunchTopCoderAppletJob job = new LaunchTopCoderAppletJob("Launching TopCoder contest applet");
		job.setUser(true);
		job.schedule();
	}
}