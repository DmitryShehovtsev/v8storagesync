package com.sdp.edt.v8storagesync.edt;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.sdp.edt.v8storagesync.support.LogHelper;
import com.sdp.edt.v8storagesync.ui.Activator;
import com.sdp.edt.v8storagesync.ui.Messages;

public class ConfigDumpManager
{
    private static final String TEMP_DUMP_DIRECTORY = ".v8storagesync"; //$NON-NLS-1$
    private static final String DEFAULT_DUMP_NAME = "1cv8"; //$NON-NLS-1$
    private static final String CONFIGURATION_FILE_EXTENSION = ".cf"; //$NON-NLS-1$
    private static final String EXTENSION_FILE_EXTENSION = ".cfe"; //$NON-NLS-1$

    public Path prepareDumpDestination(IProject project, String dumpFileName, boolean extensionProject)
        throws CoreException
    {
        IPath projectLocation = project.getLocation();
        if (projectLocation == null)
        {
            String message =
                MessageFormat.format(Messages.ConfigDumpManager_ProjectLocationNotFound, project.getName());
            throw new CoreException(createErrorStatus(message));
        }

        Path dumpDirectory = prepareDumpDirectory(project, projectLocation);
        String effectiveDumpName = (dumpFileName == null || dumpFileName.isBlank()) ? DEFAULT_DUMP_NAME : dumpFileName;
        Path destination = dumpDirectory.resolve(effectiveDumpName + resolveFileExtension(extensionProject));

        deleteExistingDumpFile(destination);
        return destination;
    }

    public void cleanupConfigDump(IProject project, Path dumpFile)
    {
        if (dumpFile == null)
        {
            return;
        }

        try
        {
            Files.deleteIfExists(dumpFile);
            deleteDumpDirectoryIfEmpty(dumpFile.getParent());
        }
        catch (IOException e)
        {
            String message = MessageFormat.format(Messages.ConfigDumpManager_DumpCleanupError, project.getName());
            LogHelper.logWarning(message, e);
        }
    }

    private Path prepareDumpDirectory(IProject project, IPath projectLocation) throws CoreException
    {
        Path dumpDirectory = Path.of(projectLocation.toOSString(), TEMP_DUMP_DIRECTORY);

        try
        {
            Files.createDirectories(dumpDirectory);
            return dumpDirectory;
        }
        catch (IOException e)
        {
            String message =
                MessageFormat.format(Messages.ConfigDumpManager_DumpDirectoryCreateError, project.getName());
            throw new CoreException(createErrorStatus(message, e));
        }
    }

    private void deleteExistingDumpFile(Path destination) throws CoreException
    {
        try
        {
            Files.deleteIfExists(destination);
        }
        catch (IOException e)
        {
            String message = MessageFormat.format(Messages.ConfigDumpManager_DumpFileDeleteError, destination);
            throw new CoreException(createErrorStatus(message, e));
        }
    }

    private void deleteDumpDirectoryIfEmpty(Path dumpDirectory) throws IOException
    {
        if (dumpDirectory == null)
        {
            return;
        }

        try
        {
            Files.deleteIfExists(dumpDirectory);
        }
        catch (DirectoryNotEmptyException e)
        {
            // Каталог может содержать оставшиеся артефакты от предыдущих операций.
        }
    }

    private String resolveFileExtension(boolean extensionProject)
    {
        return extensionProject ? EXTENSION_FILE_EXTENSION : CONFIGURATION_FILE_EXTENSION;
    }

    private IStatus createErrorStatus(String message)
    {
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
    }

    private IStatus createErrorStatus(String message, Throwable throwable)
    {
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, throwable);
    }
}