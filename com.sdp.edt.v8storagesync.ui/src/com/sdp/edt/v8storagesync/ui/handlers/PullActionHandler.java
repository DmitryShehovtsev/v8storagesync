package com.sdp.edt.v8storagesync.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.sdp.edt.v8storagesync.actions.PullAction;
import com.sdp.edt.v8storagesync.edt.V8ProjectUtils;
import com.sdp.edt.v8storagesync.support.HistoryViewUtils;
import com.sdp.edt.v8storagesync.support.LogHelper;
import com.sdp.edt.v8storagesync.support.V8StorageSyncLock;
import com.sdp.edt.v8storagesync.ui.Messages;

public class PullActionHandler
    extends AbstractHandler
{

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        if (!V8StorageSyncLock.tryLock())
        {
            return null;
        }

        Shell shell = HandlerUtil.getActiveShell(event);

        try
        {
            Repository repository = HistoryViewUtils.getRepositoryFromEvent(event);
            if (repository == null)
            {
                HistoryViewUtils.showNoRepositoryWarning(event);
                V8StorageSyncLock.release();
                return null;
            }

            IProject[] projects = V8ProjectUtils.getV8Projects(repository);
            if (projects.length == 0)
            {
                HistoryViewUtils.showNotFoundProjectWarning(shell);
                V8StorageSyncLock.release();
                return null;
            }

            PullAction action = new PullAction(repository);
            action.run();
            return null;
        }
        catch (Exception e)
        {
            V8StorageSyncLock.release();
            handleUnexpectedError(shell, e);
            throw new ExecutionException(Messages.Command_UnexpectedError, e);
        }
    }

    @Override
    public boolean isEnabled()
    {
        return !V8StorageSyncLock.isLocked();
    }

    private void handleUnexpectedError(Shell shell, Exception e)
    {
        LogHelper.logError(Messages.Command_UnexpectedError, e);

        if (shell != null && !shell.isDisposed())
        {
            String details = e.getLocalizedMessage();
            String message = (details == null || details.isBlank()) ? Messages.Command_UnexpectedError
                : Messages.Command_UnexpectedError + System.lineSeparator() + details;

            MessageDialog.openError(shell, Messages.Plugin_Header, message);
        }
    }
}