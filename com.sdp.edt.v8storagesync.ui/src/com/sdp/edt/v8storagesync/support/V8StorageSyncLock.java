package com.sdp.edt.v8storagesync.support;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

public final class V8StorageSyncLock
{

    private static final AtomicBoolean locked = new AtomicBoolean(false);

    private V8StorageSyncLock()
    {
        //
    }

    public static boolean isLocked()
    {
        return locked.get();
    }

    public static boolean tryLock()
    {
        return locked.compareAndSet(false, true);
    }

    public static void release()
    {
        locked.set(false);
        refreshCommandStatesSafely();
    }

    private static void refreshCommandStatesSafely()
    {
        Display display = Display.getDefault();
        if (display == null || display.isDisposed())
        {
            return;
        }

        Runnable refreshTask = () -> {
            if (!PlatformUI.isWorkbenchRunning())
            {
                return;
            }
            doRefreshCommandStates();
        };

        if (Display.getCurrent() == display)
        {
            refreshTask.run();
        }
        else
        {
            display.asyncExec(() -> {
                if (!display.isDisposed())
                {
                    refreshTask.run();
                }
            });
        }
    }

    private static void doRefreshCommandStates()
    {
        try
        {
            ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
            if (commandService != null)
            {
                commandService.refreshElements("com.sdp.edt.v8storagesync.ui.pullCommand", null); //$NON-NLS-1$
                commandService.refreshElements("com.sdp.edt.v8storagesync.ui.pushCommand", null); //$NON-NLS-1$
            }
        }
        catch (IllegalStateException e)
        {
            // Workbench может быть уже в процессе остановки.
        }
    }
}