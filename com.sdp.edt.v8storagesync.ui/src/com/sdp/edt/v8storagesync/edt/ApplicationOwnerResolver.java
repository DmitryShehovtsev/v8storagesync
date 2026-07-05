package com.sdp.edt.v8storagesync.edt;

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com._1c.g5.v8.dt.core.platform.IDependentProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.wiring.ServiceAccess;
import com._1c.g5.wiring.ServiceSupplier;
import com.sdp.edt.v8storagesync.ui.Activator;
import com.sdp.edt.v8storagesync.ui.Messages;

public class ApplicationOwnerResolver
{
    private final ServiceSupplier<IV8ProjectManager> v8ProjectManager =
        ServiceAccess.supplier(IV8ProjectManager.class, Activator.getDefault());

    public IProject resolveApplicationProject(IProject project) throws CoreException
    {
        IV8Project v8Project = getV8ProjectManager().getProject(project);
        if (!(v8Project instanceof IDependentProject dependentProject))
        {
            return project;
        }

        Object parentProjectObject = dependentProject.getParentProject();
        if (parentProjectObject == null)
        {
            String message = MessageFormat.format(Messages.PushAction_BaseProjectNull, project.getName());
            throw new CoreException(createErrorStatus(message));
        }

        IProject applicationOwnerProject = Adapters.adapt(parentProjectObject, IProject.class);
        if (applicationOwnerProject == null)
        {
            String message = MessageFormat.format(Messages.PushAction_BaseProjectNull, project.getName());
            throw new CoreException(createErrorStatus(message));
        }

        return applicationOwnerProject;
    }

    private IV8ProjectManager getV8ProjectManager()
    {
        return v8ProjectManager.get();
    }

    private IStatus createErrorStatus(String message)
    {
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
    }
}