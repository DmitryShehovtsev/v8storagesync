package com.sdp.edt.v8storagesync.support;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.sdp.edt.v8storagesync.ui.Activator;

public final class LogHelper
{

    private LogHelper()
    {
        //
    }

    public static IStatus logError(String message, Throwable throwable, IStatus status)
    {
        IStatus logStatus = status != null ? status : createStatus(IStatus.ERROR, message, throwable);
        log(logStatus);
        return logStatus;
    }

    public static IStatus logError(String message, Throwable throwable)
    {
        return logError(message, throwable, null);
    }

    public static IStatus logWarning(String message, Throwable throwable)
    {
        IStatus status = createStatus(IStatus.WARNING, message, throwable);
        log(status);
        return status;
    }

    public static IStatus logWarning(String message)
    {
        return logWarning(message, null);
    }

    private static IStatus createStatus(int severity, String message, Throwable throwable)
    {
        return new Status(severity, Activator.PLUGIN_ID, message, throwable);
    }

    private static void log(IStatus status)
    {
        Activator.getDefault().getLog().log(status);
    }
}