package com.sdp.edt.v8storagesync.edt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.lib.Repository;

import com._1c.g5.v8.dt.common.git.GitUtils;
import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IExtensionProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.wiring.ServiceAccess;
import com._1c.g5.wiring.ServiceSupplier;
import com.sdp.edt.v8storagesync.ui.Activator;

public final class V8ProjectUtils
{
    private static final ServiceSupplier<IV8ProjectManager> v8ProjectManager =
        ServiceAccess.supplier(IV8ProjectManager.class, Activator.getDefault());

    private V8ProjectUtils()
    {
        //
    }

    public static IProject[] getV8Projects(Repository repository)
    {
        IProject[] gitProjects = GitUtils.getProjectsInRepository(repository);
        if (gitProjects.length == 0)
        {
            return gitProjects;
        }

        IV8ProjectManager manager = v8ProjectManager.get();

        IProject configurationProject = null;
        List<IProject> extensionProjects = new ArrayList<>();

        for (IProject project : gitProjects)
        {
            IV8Project v8Project = manager.getProject(project);
            if (!isSupportedSyncProject(v8Project))
            {
                continue;
            }

            if (v8Project instanceof IConfigurationProject)
            {
                configurationProject = project;
            }
            else if (v8Project instanceof IExtensionProject)
            {
                extensionProjects.add(project);
            }
        }

        List<IProject> result = new ArrayList<>();
        if (configurationProject != null)
        {
            result.add(configurationProject);
        }
        result.addAll(extensionProjects);

        return result.toArray(new IProject[0]);
    }

    private static boolean isSupportedSyncProject(IV8Project v8Project)
    {
        return v8Project instanceof IConfigurationProject || v8Project instanceof IExtensionProject;
    }
}