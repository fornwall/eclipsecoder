package net.fornwall.eclipsecoder.languages;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;

import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.stats.CodeGenerator;
import net.fornwall.eclipsecoder.stats.ProblemStatement;
import net.fornwall.eclipsecoder.util.Utilities;
import net.fornwall.eclipsecoder.views.ProblemStatementView;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * Each implementation of this class provides support for using a programming
 * language with EclipseCoder.
 * 
 * LanguageSupport has two separate tasks:
 * <ol>
 * <li>Support the creation of appropriate Eclipse projects.</li>
 * <li>Support generation of test cases and formatting source code.</li>
 * </ol>
 */
public abstract class LanguageSupport {

	class SubmissionGetter implements Runnable {
		public String submission;

		public void run() {
			try {
				submission = getSubmission();
			} catch (Exception e) {
				Utilities.showException(e);
			}
		}
	}

	public static final String LANGUAGE_NAME_CPP = "C++";
	public static final String LANGUAGE_NAME_CSHARP = "C#";
	public static final String LANGUAGE_NAME_JAVA = "Java";
	public static final String LANGUAGE_NAME_PYTHON = "Python";
	public static final String LANGUAGE_NAME_VB = "VB";

	public static String getDefaultProjectName(ProblemStatement problemStatement, String languageName) {
		return (problemStatement.getSolutionClassName() + "-" + languageName).toLowerCase();
	}

	protected CodeGenerator codeGenerator;

	ProblemStatement problemStatement;

	private String projectName;

	private String savedSource = null;

	/**
	 * The file containing the solution which is to be submitted.
	 */
	IFile sourceFile;

	protected abstract CodeGenerator createCodeGenerator(ProblemStatement problemStatemnt);

	/**
	 * Implementations implement this method to create the language-specific
	 * parts of a project.
	 * 
	 * @return The file containing the problem class.
	 * @throws Exception
	 */
	protected abstract IFile createLanguageProject(IProject project) throws Exception;

	/**
	 * Create a new project.
	 * 
	 * Must be called in the SWT display thread.
	 * 
	 * @param problemStatement
	 *            the problem statement to create a project for
	 */
	public final CreatedProject createProject(final ProblemStatement theProblemStatement) {
		// this must run in the display thread
		Assert.isNotNull(Display.getCurrent());

		try {
			problemStatement = theProblemStatement;
			codeGenerator = createCodeGenerator(problemStatement);

			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
			final IProject myProject = workspaceRoot.getProject(getProjectName());

			if (myProject.exists()) {
				sourceFile = myProject.getFile(getSolutionFileName());
				if (sourceFile.exists()) {
					Utilities.setPerspective(getPerspectiveID());

					CreatedProject result = new CreatedProject(myProject, sourceFile, getCodeEditorID());
					result.openSourceFileInEditor();
					return result;
				}

				workbenchWindow.getShell().forceActive();
				if (Utilities.showOkCancelDialog("Malformed project exists", "The project \"" + getProjectName()
						+ "\" already exists but lacks the expected source file \"" + getSolutionFileName()
						+ "\"!\n\nPress Ok if you want to delete the project and create a new one.")) {
					myProject.delete(true, true, null);
					return createProject(theProblemStatement);
				}

				return null;
			}

			final String HTML_FILE_PATH = theProblemStatement.getSolutionClassName() + ".html";

			WorkspaceModifyOperation projectCreationOperation = new WorkspaceModifyOperation() {
				@Override
				protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
						InterruptedException {
					myProject.create(null);
					myProject.open(null);

					IFile htmlProblemStatementFile = myProject.getFile(HTML_FILE_PATH);
					htmlProblemStatementFile.create(new ByteArrayInputStream(getHtmlDescription().getBytes()), true,
							null);
					// setting this property before activating the new project
					// so that the problem statement view gets updated correctly

					EclipseCoderPlugin.getProjectPrefs(myProject).put(ProblemStatement.XML_PREFS_KEY,
							problemStatement.toXML());
				}
			};
			projectCreationOperation.run(null);

			// below prefs must be set outside the projectCreationOperation?
			try {
				IScopeContext context = new ProjectScope(myProject);
				IEclipsePreferences prefs = context.getNode(EclipseCoderPlugin.PLUGIN_ID);
				prefs.put(ProblemStatementView.PROBLEM_HTML_PATH_PREFS_KEY, myProject.getFile(HTML_FILE_PATH)
						.getProjectRelativePath().toPortableString());
				prefs.flush();
				sourceFile = createLanguageProject(myProject);
			} catch (Exception e) {
				Utilities.showException(e);
			}

			Utilities.setPerspective(getPerspectiveID());

			CreatedProject result = new CreatedProject(myProject, sourceFile, getCodeEditorID());
			result.openSourceFileInEditor();
			return result;
		} catch (Exception e) {
			Utilities.showException(e);
			return null;
		}
	}

	/**
	 * Implementations should return the ID of the editor that should be used to
	 * open the problem statement source code file.
	 * 
	 * @return The appropriate editor ID.
	 */
	protected abstract String getCodeEditorID();

	public final CodeGenerator getCodeGenerator() {
		return codeGenerator;
	}

	protected abstract String getCodeTemplate();

	/**
	 * Get the problem description in HTML form. This varies for each
	 * programming language, but different subclasses of this class does not
	 * need to reimplement this method as it takes language into account.
	 * 
	 * @return the HTML description of the problem statement
	 */
	String getHtmlDescription() {
		try {
			// change white on black to black on white
			return problemStatement.getHtmlDescription().replaceAll("bgcolor=\"#000000\"", "").replaceAll(
					"text=\"#ffffff\"", "");
		} catch (Exception e) {
			// Very unlikely
			Utilities.showException(e);
			return "<html><body><h1>Error rendering to HTML:</h1><pre>" + e.getMessage() + "</pre></body></html>";
		}
	}

	public final String getInitialSource() {
		return (savedSource == null) ? getSolutionStub() : savedSource;
	}

	/**
	 * Implementations should return the name of the programming language that
	 * they provide support for. If the support is for one of the
	 * TopCoder-supported languages C++, C#, Java or VB the named defined by the
	 * *_LANGUAGE_NAME static fields of this interface must be used.
	 * 
	 * @return the name of the programming language that the implementation
	 *         supports.
	 */
	public abstract String getLanguageName();

	/**
	 * Implementations should return the ID of the perspective to set when
	 * working on the problem in the current language.
	 * 
	 * @return The appropriate perspective ID.
	 */
	public abstract String getPerspectiveID();

	/** For subclasses */
	protected final ProblemStatement getProblemStatement() {
		return problemStatement;
	}

	/**
	 * Get the name of the project created for the problem statement.
	 * 
	 * @return the name of the newly created project.
	 */
	public final String getProjectName() {
		if (projectName == null) {
			return getDefaultProjectName(getProblemStatement(), getLanguageName());
		}

		return projectName;
	}

	protected abstract String getSolutionFileName();

	/**
	 * Get the solution stub for the problem (not including test cases).
	 * 
	 * The tags from the code template are replaced with the actual contents.
	 * 
	 * @return The solution stub for the problem.
	 */
	private String getSolutionStub() {
		return getCodeGenerator().getSolutionStub(getCodeTemplate());
	}

	/**
	 * Return the submission which will be submitted.
	 * 
	 * @return The submission.
	 */
	protected String getSubmission() throws Exception {
		try {
			return Utilities.getFileContents(sourceFile);
		} catch (CoreException e) {
			String message = String.format("There was an error reading the file '%s'.\n"
					+ "Close the problem statement and reopen to create a new project.\n\n" + "Error:\n%s", sourceFile
					.getFullPath().toOSString(), Utilities.getStackTrace(e));
			Utilities.showMessageDialog("Error reading file", message, true);
			return null;
		}
	}

	public final String getSubmissionOutsideThread() throws Exception {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.getThread().equals(Thread.currentThread())) {
			return getSubmission();
		}

		SubmissionGetter getter = new SubmissionGetter();
		display.syncExec(getter);
		return getter.submission;
	}

	public final void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	/**
	 * Setting this will override the generated source and use the supplied
	 * source instead unless null.
	 * 
	 * @param source
	 *            The supplied source (stored on TC server).
	 */
	public void setSavedSource(String source) {
		this.savedSource = source;
	}
}
