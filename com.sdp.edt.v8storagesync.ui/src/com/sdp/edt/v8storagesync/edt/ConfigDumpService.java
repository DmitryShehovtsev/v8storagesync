package com.sdp.edt.v8storagesync.edt;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IExtensionProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.platform.services.core.infobases.InfobaseAccessType;
import com._1c.g5.v8.dt.platform.services.core.infobases.export.ExportConfigurationFileException;
import com._1c.g5.v8.dt.platform.services.core.infobases.export.IExportConfigurationFileService;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.MatchingRuntimeNotFound;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.ComponentExecutorInfo;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.ILaunchableRuntimeComponent;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IRuntimeComponentManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IThickClientLauncher;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionException;
import com._1c.g5.v8.dt.platform.services.model.InfobaseReference;
import com._1c.g5.v8.dt.platform.services.model.RuntimeInstallation;
import com._1c.g5.wiring.ServiceAccess;
import com._1c.g5.wiring.ServiceSupplier;
import com.e1c.g5.dt.applications.IApplication;
import com.sdp.edt.v8storagesync.ui.Activator;
import com.sdp.edt.v8storagesync.ui.Messages;

public class ConfigDumpService
{
    private static final String ENTERPRISE_RUNTIME_TYPE =
        "com._1c.g5.v8.dt.platform.services.core.runtimeType.EnterprisePlatform"; //$NON-NLS-1$
    private static final String THICK_CLIENT_COMPONENT_TYPE =
        "com._1c.g5.v8.dt.platform.services.core.componentTypes.ThickClient"; //$NON-NLS-1$

    private final ConfigDumpManager configDumpManager;

    private final ServiceSupplier<IExportConfigurationFileService> exportConfigurationFileService =
        ServiceAccess.supplier(IExportConfigurationFileService.class, Activator.getDefault());

    private final ServiceSupplier<IResolvableRuntimeInstallationManager> resolvableRuntimeInstallationManager =
        ServiceAccess.supplier(IResolvableRuntimeInstallationManager.class, Activator.getDefault());

    private final ServiceSupplier<IRuntimeComponentManager> componentManager =
        ServiceAccess.supplier(IRuntimeComponentManager.class, Activator.getDefault());

    private final ServiceSupplier<IV8ProjectManager> v8ProjectManager =
        ServiceAccess.supplier(IV8ProjectManager.class, Activator.getDefault());

    public ConfigDumpService(ConfigDumpManager configDumpManager)
    {
        this.configDumpManager =
            Objects.requireNonNull(configDumpManager, Messages.ConfigDumpService_ConfigDumpManagerMustNotBeNull);
    }

    public Path dumpConfigurationOrExtension(IApplication application, IProject project, String fileName,
        IProgressMonitor monitor) throws InvocationTargetException, CoreException, ExportConfigurationFileException
    {
        Path destination = null;

        try
        {
            IV8Project v8Project = getV8Project(project);
            validateDumpSupported(project, v8Project);

            boolean extensionProject = v8Project instanceof IExtensionProject;
            destination = configDumpManager.prepareDumpDestination(project, fileName, extensionProject);

            String extensionName = resolveExtensionName(project, v8Project);

            InfobaseReference infobase = Adapters.adapt(application, InfobaseReference.class);
            if (infobase == null)
            {
                String message = MessageFormat.format(Messages.ConfigDumpService_InfobaseNotFound, project.getName());
                throw new CoreException(createErrorStatus(message));
            }

            RuntimeInstallation resolvedRuntime = resolveRuntime(project, infobase);
            ComponentExecutorInfo<ILaunchableRuntimeComponent, IThickClientLauncher> executor =
                resolveThickClientExecutor(resolvedRuntime);

            getExportConfigurationFileService().exportConfigurationOrExtension(infobase, executor.getComponent(),
                executor.getExecutor(), extensionName, destination, monitor);

            return destination;
        }
        catch (InvocationTargetException | CoreException | ExportConfigurationFileException e)
        {
            configDumpManager.cleanupConfigDump(project, destination);
            throw e;
        }
        catch (RuntimeException e)
        {
            configDumpManager.cleanupConfigDump(project, destination);
            throw new InvocationTargetException(e);
        }
    }

    public void cleanupConfigDump(IProject project, Path dumpFile)
    {
        configDumpManager.cleanupConfigDump(project, dumpFile);
    }

    private void validateDumpSupported(IProject project, IV8Project v8Project) throws CoreException
    {
        if (isDumpSupported(v8Project))
        {
            return;
        }

        String message = MessageFormat.format(Messages.ConfigDumpService_UnsupportedProjectType, project.getName());
        throw new CoreException(createErrorStatus(message));
    }

    private boolean isDumpSupported(IV8Project v8Project)
    {
        return v8Project instanceof IConfigurationProject || v8Project instanceof IExtensionProject;
    }

    private String resolveExtensionName(IProject project, IV8Project v8Project) throws CoreException
    {
        if (!(v8Project instanceof IExtensionProject))
        {
            return null;
        }

        String projectName = project.getName();
        int separatorIndex = projectName.indexOf('.');
        if (separatorIndex < 0 || separatorIndex == projectName.length() - 1)
        {
            String message = MessageFormat.format(Messages.ConfigDumpService_ExtensionNameNotFound, projectName);
            throw new CoreException(createErrorStatus(message));
        }

        return projectName.substring(separatorIndex + 1);
    }

    private RuntimeInstallation resolveRuntime(IProject project, InfobaseReference infobase)
        throws InvocationTargetException
    {
        try
        {
            IResolvableRuntimeInstallation resolvable = getResolvableRuntimeInstallationManager()
                .resolveByProjectAndInfobase(ENTERPRISE_RUNTIME_TYPE, project, infobase, InfobaseAccessType.UPDATE);

            return resolvable.resolve(List.of(THICK_CLIENT_COMPONENT_TYPE), infobase.getAppArch());
        }
        catch (MatchingRuntimeNotFound e)
        {
            throw new InvocationTargetException(e);
        }
    }

    private ComponentExecutorInfo<ILaunchableRuntimeComponent, IThickClientLauncher> resolveThickClientExecutor(
        RuntimeInstallation resolvedRuntime) throws InvocationTargetException
    {
        try
        {
            return getComponentManager().resolveExecutor(ILaunchableRuntimeComponent.class, IThickClientLauncher.class,
                resolvedRuntime, THICK_CLIENT_COMPONENT_TYPE);
        }
        catch (RuntimeExecutionException e)
        {
            throw new InvocationTargetException(e);
        }
    }

    private IV8Project getV8Project(IProject project)
    {
        return getV8ProjectManager().getProject(project);
    }

    private IExportConfigurationFileService getExportConfigurationFileService()
    {
        return exportConfigurationFileService.get();
    }

    private IResolvableRuntimeInstallationManager getResolvableRuntimeInstallationManager()
    {
        return resolvableRuntimeInstallationManager.get();
    }

    private IRuntimeComponentManager getComponentManager()
    {
        return componentManager.get();
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