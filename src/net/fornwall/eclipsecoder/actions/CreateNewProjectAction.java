package net.fornwall.eclipsecoder.actions;

import net.fornwall.eclipsecoder.languages.LanguageSupport;
import net.fornwall.eclipsecoder.languages.LanguageSupportFactory;
import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.stats.ProblemStatement;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class CreateNewProjectAction extends Action {

	/**
	 * TODO: Use {@link IWorkspace#validatePath(String, int)} to validate path
	 * name.
	 */
	private static class NewProjectDialog extends Dialog {

		String language;

		Combo languageCombo;

		ProblemStatement problem;

		private String projectName;

		Text projectNameField;

		public NewProjectDialog(Shell parentShell, ProblemStatement problem) {
			super(parentShell);
			this.problem = problem;
		}

		@Override
		public boolean close() {
			projectName = projectNameField.getText();
			language = languageCombo.getText();
			return super.close();
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite container = (Composite) super.createDialogArea(parent);
			final GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 2;
			container.setLayout(gridLayout);

			final Label descriptionLabel = new Label(container, SWT.NONE);
			descriptionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
			descriptionLabel.setText("Specify programming language to use and name for the project to create.");

			final Label languageLabel = new Label(container, SWT.NONE);
			GridData data = new GridData(GridData.END, GridData.CENTER, false, false);
			data.horizontalIndent = 20;
			languageLabel.setLayoutData(data);
			languageLabel.setText("Programming language:");

			languageCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			languageCombo.setItems(LanguageSupportFactory.supportedLanguages().toArray(new String[0]));
			languageCombo.setText(EclipseCoderPlugin.preferedLanguage());
			languageCombo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
			languageCombo.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					language = languageCombo.getText();
					projectNameField.setText(CreateNewProjectAction.getNewProjectNameSuggestion(problem, language));
				}

			});

			final Label nameLabel = new Label(container, SWT.NONE);
			nameLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
			nameLabel.setText("Project name:");

			projectNameField = new Text(container, SWT.BORDER);
			projectNameField.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
			projectNameField.setText(CreateNewProjectAction.getNewProjectNameSuggestion(problem, EclipseCoderPlugin
					.preferedLanguage()));

			return container;
		}

		public String getProgrammingLanguage() {
			return language;
		}

		public String getProjectName() {
			return projectName;
		}

	}

	static String getNewProjectNameSuggestion(ProblemStatement problem, String language) {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		String projectName = LanguageSupport.getDefaultProjectName(problem, language);

		if (workspaceRoot.getProject(projectName).exists())
			projectName += "-variant";
		int i = 0;
		String finalProjectName = projectName;
		while (workspaceRoot.getProject(finalProjectName).exists())
			finalProjectName = projectName + "-" + (++i);

		return finalProjectName;
	}

	private IProject project;

	public CreateNewProjectAction() {
		super("Create new project", PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_TOOL_NEW_WIZARD));
		setEnabled(false);
	}

	private IEclipsePreferences getPrefs() {
		return (project == null) ? null : EclipseCoderPlugin.getProjectPrefs(project);
	}

	@Override
	public void run() {
		if (LanguageSupportFactory.supportedLanguages().isEmpty()) {
			Utilities.showMessageDialog("Cannot create project", "No language support plug-in found.");
			return;
		}

		try {
			ProblemStatement problem = EclipseCoderPlugin.getProblemStatement(project);

			NewProjectDialog dialog = new NewProjectDialog(Utilities.getWindowShell(), problem);
			// create the window shell so the title can be set
			dialog.create();
			dialog.getShell().setText("Create new project");

			// since the Window has the blockOnOpen property set to true, it
			// will dipose of the shell upon close
			if (dialog.open() != Window.OK) {
				return;
			}

			String projectName = dialog.getProjectName();
			String language = dialog.getProgrammingLanguage();

			LanguageSupport languageSupport = LanguageSupportFactory.createLanguageSupport(language);
			languageSupport.setProjectName(projectName);
			languageSupport.createProject(problem);
		} catch (Exception e) {
			Utilities.showException(e);
		}
	}

	public void setCurrentProject(IProject project) {
		this.project = project;
		IEclipsePreferences prefs = getPrefs();

		String xmlString = (prefs == null) ? null : prefs.get(ProblemStatement.XML_PREFS_KEY, null);
		setEnabled(xmlString != null);
	}

}
