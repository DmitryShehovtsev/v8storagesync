package com.sdp.edt.v8storagesync.edt;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;

import com._1c.g5.wiring.ServiceAccess;
import com._1c.g5.wiring.ServiceSupplier;
import com.e1c.g5.dt.applications.ApplicationUpdateState;
import com.e1c.g5.dt.applications.ApplicationUpdateType;
import com.e1c.g5.dt.applications.ExecutionContext;
import com.e1c.g5.dt.applications.IApplication;
import com.e1c.g5.dt.applications.IApplicationManager;
import com.sdp.edt.v8storagesync.ui.Activator;

public class ApplicationService
{
    private static final String DEBUG_MODE = "debug"; //$NON-NLS-1$
    private static final String ACTIVE_SHELL = "activeShell"; //$NON-NLS-1$

    private final ServiceSupplier<IApplicationManager> applicationManager =
        ServiceAccess.supplier(IApplicationManager.class, Activator.getDefault());

    public Optional<IApplication> defaultApplication(IProject project)
    {
        return getApplicationManager().getDefaultApplication(project);
    }

    public ApplicationUpdateState updateApplication(IApplication application, IProgressMonitor monitor, Shell shell)
        throws InvocationTargetException
    {
        IApplicationManager appManager = getApplicationManager();
        ExecutionContext context = createExecutionContext(shell);

        try
        {
            appManager.prepare(application, DEBUG_MODE, context, monitor);
            try
            {
                return appManager.update(application, ApplicationUpdateType.INCREMENTAL, context, monitor);
            }
            finally
            {
                appManager.cleanup(application, context, monitor);
            }
        }
        catch (RuntimeException e)
        {
            throw new InvocationTargetException(e);
        }
    }

    private ExecutionContext createExecutionContext(Shell shell)
    {
        Map<String, Object> parameters = new HashMap<>();
        if (shell != null && !shell.isDisposed())
        {
            parameters.put(ACTIVE_SHELL, shell);
        }
        return new ExecutionContext(parameters);
    }

    private IApplicationManager getApplicationManager()
    {
        return applicationManager.get();
    }
}