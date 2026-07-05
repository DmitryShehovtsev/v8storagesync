package com.sdp.edt.v8storagesync.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.lib.Repository;

import com.sdp.edt.v8storagesync.ui.Messages;

public abstract class AbstractActions
{
    protected final Repository repo;

    protected AbstractActions(Repository repository)
    {
        this.repo = Objects.requireNonNull(repository, Messages.AbstractActions_RepositoryMustNotBeNull);
    }

    public abstract void run() throws InvocationTargetException;

    public abstract String header();

    public abstract List<String> commandArguments();

    public String standardInput()
    {
        return null;
    }

    public IStatus beforeRunJob(IProject project, IProgressMonitor monitor) throws InvocationTargetException
    {
        return Status.OK_STATUS;
    }

    public void afterProjectRun(IProject project, IProgressMonitor monitor)
    {
        //
    }

    public Repository getRepository()
    {
        return repo;
    }
}