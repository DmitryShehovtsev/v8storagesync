package com.sdp.edt.v8storagesync.process;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import com.sdp.edt.v8storagesync.ui.Activator;
import com.sdp.edt.v8storagesync.ui.Messages;

public final class ToolCommandResolver
{
    private static final String ENV_PATH = "PATH"; //$NON-NLS-1$
    private static final String ENV_PATHEXT = "PATHEXT"; //$NON-NLS-1$

    private static final String DEFAULT_WINDOWS_PATHEXT = ".COM;.EXE;.BAT;.CMD"; //$NON-NLS-1$

    private static final String WINDOWS_CMD = "cmd.exe"; //$NON-NLS-1$
    private static final String WINDOWS_CMD_DISABLE_AUTORUN = "/d"; //$NON-NLS-1$
    private static final String WINDOWS_CMD_EXECUTE = "/c"; //$NON-NLS-1$

    public List<String> buildCommand(String configuredCommand, List<String> arguments, String workingDirectory)
        throws CoreException
    {
        String normalizedCommand = normalizeConfiguredCommand(configuredCommand);
        Path resolvedCommand = resolveCommand(normalizedCommand, workingDirectory);

        List<String> command = new ArrayList<>();

        if (isWindowsBatchFile(resolvedCommand))
        {
            command.add(WINDOWS_CMD);
            command.add(WINDOWS_CMD_DISABLE_AUTORUN);
            command.add(WINDOWS_CMD_EXECUTE);
        }

        command.add(resolvedCommand.toString());
        command.addAll(arguments);

        return command;
    }

    private String normalizeConfiguredCommand(String configuredCommand) throws CoreException
    {
        if (configuredCommand == null || configuredCommand.trim().isEmpty())
        {
            throw new CoreException(createErrorStatus(Messages.ScriptRunnerJob_CommandIsEmpty));
        }

        return configuredCommand.trim();
    }

    private Path resolveCommand(String configuredCommand, String workingDirectory) throws CoreException
    {
        try
        {
            Path configuredPath = Paths.get(configuredCommand);

            if (configuredPath.isAbsolute() || containsPathSeparator(configuredCommand))
            {
                return resolveFromExplicitPath(configuredPath, workingDirectory, configuredCommand);
            }
        }
        catch (InvalidPathException e)
        {
            String message = MessageFormat.format(Messages.ScriptRunnerJob_CommandInvalidPath, configuredCommand);
            throw new CoreException(createErrorStatus(message, e));
        }

        return resolveFromPathEnvironment(configuredCommand).orElseThrow(() -> {
            String message = MessageFormat.format(Messages.ScriptRunnerJob_CommandNotFound, configuredCommand);
            return new CoreException(createErrorStatus(message));
        });
    }

    private Path resolveFromExplicitPath(Path configuredPath, String workingDirectory, String configuredCommand)
        throws CoreException
    {
        Path candidate = configuredPath;
        if (!candidate.isAbsolute())
        {
            candidate = Paths.get(workingDirectory).resolve(candidate);
        }

        Optional<Path> resolved = resolveExistingPath(candidate);
        if (resolved.isEmpty())
        {
            String message = MessageFormat.format(Messages.ScriptRunnerJob_CommandNotFound, configuredCommand);
            throw new CoreException(createErrorStatus(message));
        }

        Path normalized = resolved.get().toAbsolutePath().normalize();
        validateExecutability(normalized);

        return normalized;
    }

    private Optional<Path> resolveFromPathEnvironment(String commandName)
    {
        String pathValue = System.getenv(ENV_PATH);
        if (pathValue == null || pathValue.isBlank())
        {
            return Optional.empty();
        }

        String[] pathEntries = pathValue.split(Pattern.quote(File.pathSeparator));
        for (String entry : pathEntries)
        {
            if (entry == null || entry.isBlank())
            {
                continue;
            }

            Path directory;
            try
            {
                directory = Paths.get(entry.trim());
            }
            catch (InvalidPathException e)
            {
                continue;
            }

            Optional<Path> resolved = resolveExistingPath(directory.resolve(commandName));
            if (resolved.isPresent())
            {
                Path normalized = resolved.get().toAbsolutePath().normalize();

                try
                {
                    validateExecutability(normalized);
                    return Optional.of(normalized);
                }
                catch (CoreException e)
                {
                    //
                }
            }
        }

        return Optional.empty();
    }

    private Optional<Path> resolveExistingPath(Path candidate)
    {
        if (isRegularFile(candidate))
        {
            return Optional.of(candidate);
        }

        if (isWindows() && !hasExtension(candidate))
        {
            for (String extension : getWindowsPathExtensions())
            {
                Path candidateWithExtension = Paths.get(candidate.toString() + extension);
                if (isRegularFile(candidateWithExtension))
                {
                    return Optional.of(candidateWithExtension);
                }
            }
        }

        return Optional.empty();
    }

    private void validateExecutability(Path commandPath) throws CoreException
    {
        if (isWindows())
        {
            return;
        }

        if (!Files.isExecutable(commandPath))
        {
            String message = MessageFormat.format(Messages.ScriptRunnerJob_CommandNotExecutable, commandPath);
            throw new CoreException(createErrorStatus(message));
        }
    }

    private boolean isRegularFile(Path path)
    {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    private boolean hasExtension(Path path)
    {
        Path fileName = path.getFileName();
        if (fileName == null)
        {
            return false;
        }

        String name = fileName.toString();
        int lastDotIndex = name.lastIndexOf('.');
        return lastDotIndex > 0 && lastDotIndex < name.length() - 1;
    }

    private boolean containsPathSeparator(String command)
    {
        return command.contains("/") || command.contains("\\"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private boolean isWindows()
    {
        return Platform.OS_WIN32.equals(Platform.getOS());
    }

    private boolean isWindowsBatchFile(Path commandPath)
    {
        if (!isWindows())
        {
            return false;
        }

        String fileName = commandPath.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".bat") || fileName.endsWith(".cmd"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private List<String> getWindowsPathExtensions()
    {
        String pathExt = System.getenv(ENV_PATHEXT);
        if (pathExt == null || pathExt.isBlank())
        {
            pathExt = DEFAULT_WINDOWS_PATHEXT;
        }

        return Arrays.stream(pathExt.split(";")) //$NON-NLS-1$
            .map(String::trim)
            .filter(extension -> !extension.isEmpty())
            .map(extension -> extension.startsWith(".") ? extension : "." + extension) //$NON-NLS-1$ //$NON-NLS-2$
            .toList();
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