package com.sdp.edt.v8storagesync.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com._1c.g5.v8.dt.platform.services.core.infobases.export.ExportConfigurationFileException;
import com.e1c.g5.dt.applications.ApplicationUpdateState;
import com.e1c.g5.dt.applications.IApplication;
import com.sdp.edt.v8storagesync.edt.ApplicationOwnerResolver;
import com.sdp.edt.v8storagesync.edt.ApplicationService;
import com.sdp.edt.v8storagesync.edt.ConfigDumpManager;
import com.sdp.edt.v8storagesync.edt.ConfigDumpService;
import com.sdp.edt.v8storagesync.git.GitActions;
import com.sdp.edt.v8storagesync.jobs.ScriptRunnerJob;
import com.sdp.edt.v8storagesync.support.LogHelper;
import com.sdp.edt.v8storagesync.ui.Activator;
import com.sdp.edt.v8storagesync.ui.Messages;
import com.sdp.edt.v8storagesync.ui.dialogs.ICommitHandler;
import com.sdp.edt.v8storagesync.ui.dialogs.PushDialog;

public class PushAction
    extends AbstractActions
{
    private String hash;
    private String commitMessage;
    private Shell shell;

    private final ApplicationOwnerResolver applicationOwnerResolver;
    private final ApplicationService ApplicationService;
    private final ConfigDumpService ConfigDumpService;

    private final Map<IProject, IApplication> applicationsByOwnerProject = new HashMap<>();
    private final Set<IProject> updatedOwnerProjects = new HashSet<>();
    private final Map<IProject, Path> dumpFilesByProject = new HashMap<>();

    public PushAction(Repository repository, Shell shell)
    {
        super(repository);

        this.shell = shell;
        this.applicationOwnerResolver = new ApplicationOwnerResolver();
        this.ApplicationService = new ApplicationService();

        ConfigDumpManager ConfigDumpManager = new ConfigDumpManager();
        this.ConfigDumpService = new ConfigDumpService(ConfigDumpManager);
    }

    @Override
    public void run() throws InvocationTargetException
    {
        Shell parentShell = resolveDialogShell();
        if (parentShell == null)
        {
            throw new InvocationTargetException(new IllegalStateException(Messages.PushAction_NoValidShellAvailable));
        }

        this.shell = parentShell;

        ICommitHandler callback = (hash, commitMessage) -> {
            this.hash = hash;
            this.commitMessage = commitMessage;
            ScriptRunnerJob job = new ScriptRunnerJob(this);
            job.schedule();
        };

        PushDialog.show(repo, parentShell, callback);
    }

    @Override
    public String header()
    {
        return Messages.ScriptRunnerJob_HeaderPush;
    }

    @Override
    public List<String> commandArguments()
    {
        List<String> args = new ArrayList<>();
        args.add("v8storage"); //$NON-NLS-1$
        args.add("push"); //$NON-NLS-1$
        args.add("-h"); //$NON-NLS-1$
        args.add(hash != null ? hash : ""); //$NON-NLS-1$
        args.add("--message-stdin"); //$NON-NLS-1$
        return args;
    }

    @Override
    public String standardInput()
    {
        return commitMessage;
    }

    @Override
    public IStatus beforeRunJob(IProject project, IProgressMonitor subMonitor) throws InvocationTargetException
    {
        if (subMonitor.isCanceled())
        {
            return Status.CANCEL_STATUS;
        }

        final IProject applicationOwnerProject;
        try
        {
            applicationOwnerProject = applicationOwnerResolver.resolveApplicationProject(project);
        }
        catch (CoreException e)
        {
            return e.getStatus();
        }

        final IApplication application;
        try
        {
            application = resolveApplication(applicationOwnerProject);
        }
        catch (CoreException e)
        {
            return e.getStatus();
        }

        if (!updatedOwnerProjects.contains(applicationOwnerProject))
        {
            IStatus updateStatus = doUpdate(application, subMonitor);
            if (!updateStatus.isOK())
            {
                return updateStatus;
            }
            updatedOwnerProjects.add(applicationOwnerProject);
        }

        return doConfigDump(application, project, subMonitor);
    }

    @Override
    public void afterProjectRun(IProject project, IProgressMonitor monitor)
    {
        Path dumpFile = dumpFilesByProject.remove(project);
        ConfigDumpService.cleanupConfigDump(project, dumpFile);
    }

    private IApplication resolveApplication(IProject applicationOwnerProject) throws CoreException
    {
        IApplication cachedApplication = applicationsByOwnerProject.get(applicationOwnerProject);
        if (cachedApplication != null)
        {
            return cachedApplication;
        }

        Optional<IApplication> optionalApplication = ApplicationService.defaultApplication(applicationOwnerProject);
        if (optionalApplication.isEmpty())
        {
            String msg =
                MessageFormat.format(Messages.PushAction_ApplicationNotFound, applicationOwnerProject.getName());
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg));
        }

        IApplication application = optionalApplication.get();
        applicationsByOwnerProject.put(applicationOwnerProject, application);
        return application;
    }

    private IStatus doUpdate(IApplication application, IProgressMonitor subMonitor) throws InvocationTargetException
    {
        ApplicationUpdateState updateState = ApplicationService.updateApplication(application, subMonitor, shell);

        switch (updateState)
        {
        case UPDATED:
            return new Status(IStatus.OK, Activator.PLUGIN_ID, Messages.PushAction_Updated);

        case INCREMENTAL_UPDATE_REQUIRED:
            return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, Messages.PushAction_NeedsRepeated);

        default:
            String msg = MessageFormat.format(Messages.PushAction_UnexpectedValue, updateState);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg);
        }
    }

    private IStatus doConfigDump(IApplication application, IProject project, IProgressMonitor subMonitor)
        throws InvocationTargetException
    {
        if (subMonitor.isCanceled())
        {
            return Status.CANCEL_STATUS;
        }

        String projectName = project.getName();
        String msgDump = MessageFormat.format(Messages.PushAction_DumpingConf, projectName);
        subMonitor.subTask(msgDump);

        String dumpName;
        try
        {
            dumpName = new GitActions(repo).getHeadHash();
        }
        catch (IOException e)
        {
            throw new InvocationTargetException(e, Messages.PushAction_HeadHashReadError);
        }

        try
        {
            Path dumpFile =
                ConfigDumpService.dumpConfigurationOrExtension(application, project, dumpName, subMonitor);
            dumpFilesByProject.put(project, dumpFile);

            String msg = MessageFormat.format(Messages.PushAction_ConfigDumpSuccess, projectName);
            return new Status(IStatus.OK, Activator.PLUGIN_ID, msg);
        }
        catch (InvocationTargetException | ExportConfigurationFileException | CoreException e)
        {
            String msg = MessageFormat.format(Messages.PushAction_ConfigDumpError, projectName);
            return LogHelper.logError(msg, e);
        }
    }

    private Shell resolveDialogShell()
    {
        if (shell != null && !shell.isDisposed())
        {
            return shell;
        }

        Display display = Display.getDefault();
        if (display == null || display.isDisposed())
        {
            return null;
        }

        return display.getActiveShell();
    }
}
