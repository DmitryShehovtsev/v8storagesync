package com.sdp.edt.v8storagesync.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.Repository;

import com.sdp.edt.v8storagesync.jobs.ScriptRunnerJob;
import com.sdp.edt.v8storagesync.ui.Messages;

public class PullAction
    extends AbstractActions
{

    public PullAction(Repository repository)
    {
        super(repository);
    }

    @Override
    public void run()
    {
        ScriptRunnerJob job = new ScriptRunnerJob(this);
        job.schedule();
    }

    @Override
    public String header()
    {
        return Messages.ScriptRunnerJob_HeaderPull;
    }

    @Override
    public List<String> commandArguments()
    {
        List<String> args = new ArrayList<>();
        args.add("v8storage"); //$NON-NLS-1$
        args.add("pull"); //$NON-NLS-1$
        return args;
    }
}
