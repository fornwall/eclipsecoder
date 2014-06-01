package net.fornwall.eclipsecoder.views;

import net.fornwall.eclipsecoder.actions.CreateNewProjectAction;
import net.fornwall.eclipsecoder.actions.StartTopCoderAppletAction;
import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.stats.ProblemStatement;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

/**
 * A view which shows the problem statement of the TopCoder problem associated with the active project in the workbench.
 * If no TopCoder problem is associated with the current problem a welcome text with instructions is is shown instead.
 */
public class ProblemStatementView extends ViewPart {

	// the text to show when no TopCoder problem is associated with the current
	// project
	private static String INITIAL_HTML = "<html><body><h1>Welcome to EclipseCoder!</h1><ul>"
			+ "<li>When a problem is selected, its problem statement will be shown here</li>"
			+ "<li>Start the TopCoder contest applet by pressing the start button on the toolbar of this view</li>"
			+ "<li>Open a problem statement</li>" + "<li>Work on and solve the problem in Eclipse</li>"
			+ "<li>Submit your solution and profit by using the normal submit button inside the arena</li>"
			+ "</ul></body></html>";

	/**
	 * When a project is associated with a TopCoder problem, the project-relative path should be stored under this key
	 * under the project preferences in a portable format.
	 * 
	 * @see IResource#getProjectRelativePath()
	 * @see IPath#toPortableString()
	 */
	public static final String PROBLEM_HTML_PATH_PREFS_KEY = "problemStatement";

	public static final String VIEW_ID = ProblemStatementView.class.getCanonicalName();

	Browser browser;

	private IProject lastProject;

	ISelectionListener pageSelectionListener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			updateContent();
		}
	};

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillActions(bars.getMenuManager());
		fillActions(bars.getToolBarManager());
	}

	@Override
	public void createPartControl(Composite parent) {
		browser = new Browser(parent, SWT.NONE);
		browser.setText(INITIAL_HTML);
		updateContent();
		contributeToActionBars();
		hookPageSelection();
	}

	@Override
	public void dispose() {
		super.dispose();
		getSite().getPage().removePostSelectionListener(pageSelectionListener);
	}

	private void fillActions(IContributionManager manager) {
		manager.add(startTopCoderAppletAction);
		manager.add(createNewProjectAction);
		if (manager instanceof IMenuManager) {
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		}
	}

	private void hookPageSelection() {
		getSite().getPage().addPostSelectionListener(pageSelectionListener);
	}

	@Override
	public void setFocus() {
		browser.setFocus();
	}

	private CreateNewProjectAction createNewProjectAction = new CreateNewProjectAction();

	private StartTopCoderAppletAction startTopCoderAppletAction = new StartTopCoderAppletAction();

	/**
	 * Update the content displayed in this view if the active project has changed.
	 */
	void updateContent() {
		IEditorPart activeEditor = getSite().getPage().getActiveEditor();
		if (activeEditor == null) {
			if (browser.getUrl().length() == 0) {
				browser.setText(INITIAL_HTML);
				createNewProjectAction.setCurrentProject(null);
			}
			return;
		}
		IEditorInput editorInput = activeEditor.getEditorInput();
		if (editorInput instanceof IFileEditorInput) {
			IFile editorFile = ((IFileEditorInput) editorInput).getFile();
			IProject project = editorFile.getProject();

			if (lastProject != project && project.exists()) {
				lastProject = project;
				try {
					IEclipsePreferences prefs = EclipseCoderPlugin.getProjectPrefs(project);

					createNewProjectAction.setCurrentProject(project);

					String location = prefs.get(ProblemStatementView.PROBLEM_HTML_PATH_PREFS_KEY, null);

					if (location == null) {
						browser.setText(INITIAL_HTML);
						setContentDescription("");
					} else {
						ProblemStatement statement = EclipseCoderPlugin.getProblemStatement(project);
						if (statement != null && statement.getContestName() != null) {
							setContentDescription(statement.getContestName());
						} else {
							setContentDescription("");
						}

						final String url = project.getLocation().append(Path.fromPortableString(location)).toOSString();
						Utilities.runInDisplayThread(new Runnable() {
							public void run() {
								browser.setUrl(url);
							}
						});
					}
				} catch (Exception e) {
					Utilities.showException(e);
				}
			}
		}
	}
}