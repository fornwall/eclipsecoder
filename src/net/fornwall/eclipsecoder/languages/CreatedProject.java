package net.fornwall.eclipsecoder.languages;

import net.fornwall.eclipsecoder.util.Utilities;
import net.fornwall.eclipsecoder.views.ProblemStatementView;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

public class CreatedProject {

	IFile sourceFile;
	private IProject project;
	String editorId;

	public CreatedProject(IProject project, IFile sourceFile, String editorId) {
		this.project = project;
		this.sourceFile = sourceFile;
		this.editorId = editorId;
	}

	public IFile getSourceFile() {
		return sourceFile;
	}

	public IProject getProject() {
		return project;
	}

	public String getDescription() {
		return sourceFile.getProjectRelativePath().toString() + " inside project " + project.getName();
	}

	public void openSourceFileInEditor() {
		Utilities.runInDisplayThread(new Runnable() {

			public void run() {
				try {
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					window.getActivePage().showView(ProblemStatementView.VIEW_ID);
					window.getActivePage().openEditor(new FileEditorInput(sourceFile), editorId);
				} catch (PartInitException e) {
					Utilities.showException(e);
				}
			}
		});
	}
}
