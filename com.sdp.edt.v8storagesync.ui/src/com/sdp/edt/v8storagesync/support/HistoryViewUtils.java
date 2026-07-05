package com.sdp.edt.v8storagesync.support;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com._1c.g5.v8.dt.common.git.GitUtils;
import com.sdp.edt.v8storagesync.ui.Messages;

public final class HistoryViewUtils
{

    private HistoryViewUtils()
    {
        //
    }

    public static void showNoRepositoryWarning(ExecutionEvent event)
    {
        Shell shell = HandlerUtil.getActiveShell(event);
        if (shell != null)
        {
            MessageDialog.openWarning(shell, Messages.Plugin_Header, Messages.HistoryViewUtils_FailedResolveGitRepo);
        }
    }

    public static void showNotFoundProjectWarning(Shell shell)
    {
        if (shell != null && !shell.isDisposed())
        {
            MessageDialog.openWarning(shell, Messages.Plugin_Header, Messages.HistoryViewUtils_NotFoundProject);
        }
    }

    public static Repository getRepositoryFromEvent(ExecutionEvent event)
    {
        if (event == null)
        {
            return null;
        }

        Repository repo = tryGetFromHistoryView(event);
        if (repo != null)
        {
            return repo;
        }

        repo = tryGetFromSelection(event);
        if (repo != null)
        {
            return repo;
        }

        return tryGetFromActiveEditor(event);
    }

    private static Repository tryGetFromHistoryView(ExecutionEvent event)
    {
        IWorkbenchPart part = HandlerUtil.getActivePart(event);
        if (!(part instanceof IHistoryView))
        {
            return null;
        }

        IHistoryView historyView = (IHistoryView)part;
        IHistoryPage page = historyView.getHistoryPage();
        if (page == null)
        {
            return null;
        }

        return adaptToRepository(page.getInput());
    }

    private static Repository tryGetFromSelection(ExecutionEvent event)
    {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection) || ((IStructuredSelection)selection).isEmpty())
        {
            return null;
        }

        Object element = ((IStructuredSelection)selection).getFirstElement();
        return adaptToRepository(element);
    }

    private static Repository tryGetFromActiveEditor(ExecutionEvent event)
    {
        IWorkbenchPart part = HandlerUtil.getActivePart(event);
        if (part == null)
        {
            return null;
        }

        IResource resource = Adapters.adapt(part, IResource.class);
        if (resource != null)
        {
            return GitUtils.getGitRepository(resource);
        }

        IProject project = Adapters.adapt(part, IProject.class);
        if (project != null)
        {
            return GitUtils.getGitRepository(project);
        }

        return null;
    }

    private static Repository adaptToRepository(Object element)
    {
        if (element == null)
        {
            return null;
        }

        Repository repo = Adapters.adapt(element, Repository.class);
        if (repo != null)
        {
            return repo;
        }

        IResource resource = Adapters.adapt(element, IResource.class);
        if (resource != null)
        {
            Repository r = GitUtils.getGitRepository(resource);
            if (r != null)
            {
                return r;
            }
        }

        IProject project = Adapters.adapt(element, IProject.class);
        if (project != null)
        {
            return GitUtils.getGitRepository(project);
        }

        return null;
    }
}
