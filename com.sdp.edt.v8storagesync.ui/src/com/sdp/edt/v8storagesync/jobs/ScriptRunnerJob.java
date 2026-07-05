package com.sdp.edt.v8storagesync.jobs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.progress.IProgressConstants;

import com._1c.g5.v8.dt.common.runtime.ProgressMonitors;
import com.e1c.g5.dt.applications.ApplicationException;
import com.sdp.edt.v8storagesync.actions.AbstractActions;
import com.sdp.edt.v8storagesync.edt.V8ProjectUtils;
import com.sdp.edt.v8storagesync.process.ToolCommandResolver;
import com.sdp.edt.v8storagesync.support.HistoryViewUtils;
import com.sdp.edt.v8storagesync.support.LogHelper;
import com.sdp.edt.v8storagesync.support.V8StorageSyncLock;
import com.sdp.edt.v8storagesync.ui.Activator;
import com.sdp.edt.v8storagesync.ui.Messages;

public class ScriptRunnerJob
    extends Job
{
    private static final long PROCESS_WAIT_MS = 100;
    private static final String CONSOLE_NAME = "V8 Storage Sync Output"; //$NON-NLS-1$

    private final AbstractActions action;
    private final ToolCommandResolver commandResolver = new ToolCommandResolver();

    public ScriptRunnerJob(AbstractActions action)
    {
        super(action.header());
        this.action = action;
        setUser(true);
        setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        try
        {
            ProgressMonitors.runAsTask(action.header(), IProgressMonitor.UNKNOWN, monitor, subMonitor -> {
                runWithMonitor(subMonitor.split(1));
            });
            return Status.OK_STATUS;
        }
        catch (OperationCanceledException e)
        {
            return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, Messages.ScriptRunnerJob_UserCancel, e);
        }
        catch (CoreException e)
        {
            return e.getStatus();
        }
        catch (Exception e)
        {
            return handleException(e);
        }
        finally
        {
            V8StorageSyncLock.release();
            monitor.done();
        }
    }

    @Override
    protected void canceling()
    {
        V8StorageSyncLock.release();
        super.canceling();
    }

    private void runWithMonitor(IProgressMonitor monitor) throws Exception
    {
        checkMonitorCanceled(monitor);

        IProject[] projects = V8ProjectUtils.getV8Projects(action.getRepository());
        if (projects.length == 0)
        {
            Display display = Display.getDefault();
            if (display != null && !display.isDisposed())
            {
                display.asyncExec(() -> {
                    Shell shell = display.getActiveShell();
                    HistoryViewUtils.showNotFoundProjectWarning(shell);
                });
            }

            throw new CoreException(
                new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.HistoryViewUtils_NotFoundProject));
        }

        String configuredCommand = Activator.getDefault().getUtilName();

        for (IProject project : projects)
        {
            checkMonitorCanceled(monitor);
            updateProjectTask(monitor, project);

            try
            {
                IStatus beforeRunStatus = action.beforeRunJob(project, monitor);
                ensureOk(beforeRunStatus);

                updateProjectTask(monitor, project);

                String cwd = project.getLocation().toOSString();
                IStatus executionStatus = executeScript(configuredCommand, cwd, monitor);
                ensureOk(executionStatus);
            }
            finally
            {
                action.afterProjectRun(project, monitor);
                safeRefreshProject(project, monitor);
            }
        }
    }

    private void updateProjectTask(IProgressMonitor monitor, IProject project)
    {
        String taskName = MessageFormat.format(Messages.ScriptRunnerJob_ProjectTaskName, project.getName());
        monitor.setTaskName(taskName);
    }

    private void checkMonitorCanceled(IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            throw new OperationCanceledException(Messages.ScriptRunnerJob_UserCancel);
        }
    }

    private void ensureOk(IStatus status) throws CoreException
    {
        if (status == null || status.isOK())
        {
            return;
        }

        throw new CoreException(status);
    }

    private IStatus handleException(Exception e)
    {
        Throwable cause = e.getCause() != null ? e.getCause() : e;

        if (cause instanceof ApplicationException applicationError)
        {
            IStatus status = applicationError.getStatus();

            if (status != null)
            {
                if (status.matches(IStatus.CANCEL))
                {
                    return status;
                }

                String errorMessage = firstNonBlank(status.getMessage(), applicationError.getMessage());
                return LogHelper.logError(errorMessage, applicationError, status);
            }

            String errorMessage = firstNonBlank(applicationError.getMessage());
            return LogHelper.logError(errorMessage, applicationError);
        }

        String errorMessage = firstNonBlank(e.getMessage(), cause.getMessage());
        return LogHelper.logError(errorMessage, e);
    }

    private String firstNonBlank(String... values)
    {
        for (String value : values)
        {
            if (value != null && !value.isBlank())
            {
                return value;
            }
        }
        return Messages.Command_UnexpectedError;
    }

    private IStatus executeScript(String configuredCommand, String cwd, IProgressMonitor monitor)
    {
        checkMonitorCanceled(monitor);

        Process process = null;

        try
        {
            ProcessBuilder pb =
                new ProcessBuilder(commandResolver.buildCommand(configuredCommand, action.commandArguments(), cwd));
            pb.redirectErrorStream(true);
            pb.directory(new File(cwd));

            process = pb.start();
            writeStandardInput(process, action.standardInput());

            return readProcessOutput(process, monitor);
        }
        catch (CoreException e)
        {
            return e.getStatus();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();

            if (monitor.isCanceled())
            {
                return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, Messages.ScriptRunnerJob_UserCancel, e);
            }

            String errorMsg = MessageFormat.format(Messages.ScriptRunnerJob_ErrorDuringExecution, e.getMessage());
            return LogHelper.logError(errorMsg, e);
        }
        catch (IOException e)
        {
            String errorMsg = MessageFormat.format(Messages.ScriptRunnerJob_ErrorDuringExecution, e.getMessage());
            return LogHelper.logError(errorMsg, e);
        }
        finally
        {
            destroyProcess(process);
        }
    }

    private void writeStandardInput(Process process, String standardInput) throws IOException
    {
        try (Writer writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))
        {
            if (standardInput != null)
            {
                writer.write(standardInput);
            }
            writer.flush();
        }
    }

    private void destroyProcess(Process process)
    {
        if (process == null || !process.isAlive())
        {
            return;
        }

        process.destroy();

        try
        {
            if (!process.waitFor(PROCESS_WAIT_MS, TimeUnit.MILLISECONDS))
            {
                process.destroyForcibly();
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private IStatus readProcessOutput(Process process, IProgressMonitor monitor)
        throws IOException, InterruptedException
    {
        MessageConsole console = findOrCreateConsole();
        console.clearConsole();

        Charset charset = StandardCharsets.UTF_8;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset));
            MessageConsoleStream outputStream = console.newMessageStream())
        {
            while (process.isAlive())
            {
                if (monitor.isCanceled())
                {
                    monitor.subTask(Messages.ScriptRunnerJob_UserCancel);
                    return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, Messages.ScriptRunnerJob_UserCancel);
                }

                drainAvailableLines(reader, outputStream, monitor);
                process.waitFor(PROCESS_WAIT_MS, TimeUnit.MILLISECONDS);
            }

            drainRemainingLines(reader, outputStream, monitor);

            int exitCode = process.exitValue();
            if (exitCode != 0)
            {
                String errorMsg = MessageFormat.format(Messages.ScriptRunnerJob_ErrorCode, exitCode);
                return LogHelper.logError(errorMsg, null);
            }

            monitor.subTask(Messages.ScriptRunnerJob_Done);
            return Status.OK_STATUS;
        }
    }

    private void drainAvailableLines(BufferedReader reader, MessageConsoleStream outputStream, IProgressMonitor monitor)
        throws IOException
    {
        while (reader.ready())
        {
            String line = reader.readLine();
            if (line == null)
            {
                return;
            }

            outputStream.println(line);
            monitor.subTask(line);
        }
    }

    private void drainRemainingLines(BufferedReader reader, MessageConsoleStream outputStream, IProgressMonitor monitor)
        throws IOException
    {
        String line;
        while ((line = reader.readLine()) != null)
        {
            outputStream.println(line);
            monitor.subTask(line);
        }
    }

    private void safeRefreshProject(IProject project, IProgressMonitor monitor)
    {
        try
        {
            project.refreshLocal(IProject.DEPTH_INFINITE, monitor);
        }
        catch (CoreException e)
        {
            String message = MessageFormat.format(Messages.ScriptRunnerJob_ProjectRefreshError, project.getName());
            LogHelper.logWarning(message, e);
        }
    }

    private MessageConsole findOrCreateConsole()
    {
        IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
        for (IConsole console : consoleManager.getConsoles())
        {
            if (CONSOLE_NAME.equals(console.getName()))
            {
                return (MessageConsole)console;
            }
        }

        MessageConsole newConsole = new MessageConsole(CONSOLE_NAME, null);
        consoleManager.addConsoles(new IConsole[] { newConsole });
        return newConsole;
    }
}