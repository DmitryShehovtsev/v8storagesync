package com.sdp.edt.v8storagesync.git;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Objects;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import com.sdp.edt.v8storagesync.ui.Messages;

public class GitActions
{
    private static final String HEAD_REF = "HEAD"; //$NON-NLS-1$

    private final Repository repository;

    public GitActions(Repository repository)
    {
        this.repository = Objects.requireNonNull(repository, Messages.GitActions_RepositoryMustNotBeNull);
    }

    public boolean hasHeadCommit() throws InvocationTargetException
    {
        try
        {
            return resolveHeadId() != null;
        }
        catch (IOException e)
        {
            throw new InvocationTargetException(e);
        }
    }

    public String getCommitContextInfo() throws InvocationTargetException
    {
        try (RevWalk revWalk = new RevWalk(repository))
        {
            ObjectId headId = resolveHeadId();
            if (headId == null)
            {
                return Messages.GitActions_NoHeadCommit;
            }

            RevCommit headCommit = revWalk.parseCommit(headId);
            String headInfo = MessageFormat.format(Messages.GitActions_HeadCommitInfo, headCommit.getId().getName());

            ParentCommitInfo parentInfo = resolveDefaultParentInfo(revWalk, headCommit);
            if (parentInfo == null)
            {
                return headInfo + System.lineSeparator() + Messages.GitActions_NoParentCommit;
            }

            String parentText = parentInfo.defaultFromMergedBranch()
                ? MessageFormat.format(Messages.GitActions_MergeParentCommitInfo, parentInfo.commitId())
                : MessageFormat.format(Messages.GitActions_ParentCommitInfo, parentInfo.commitId());

            return headInfo + System.lineSeparator() + parentText;
        }
        catch (IOException e)
        {
            throw new InvocationTargetException(e);
        }
    }

    public String getHeadHash() throws IOException
    {
        ObjectId headId = resolveHeadId();
        if (headId == null)
        {
            throw new IOException(Messages.GitActions_NoHeadCommit);
        }
        return headId.getName();
    }

    public String getParentHash() throws InvocationTargetException
    {
        try (RevWalk revWalk = new RevWalk(repository))
        {
            ObjectId headId = resolveHeadId();
            if (headId == null)
            {
                return ""; //$NON-NLS-1$
            }

            RevCommit headCommit = revWalk.parseCommit(headId);
            ParentCommitInfo parentInfo = resolveDefaultParentInfo(revWalk, headCommit);
            return parentInfo != null ? parentInfo.commitId() : ""; //$NON-NLS-1$
        }
        catch (IOException e)
        {
            throw new InvocationTargetException(e);
        }
    }

    private ObjectId resolveHeadId() throws IOException
    {
        return repository.resolve(HEAD_REF);
    }

    /**
     * Бизнес-правило:
     * - если у HEAD один родитель, используем его;
     * - если HEAD является merge commit, по умолчанию выбираем второго родителя,
     *   при этом в типовом сценарии это будет merged branch (обычно master),
     *   относительно которого нужно вычислять изменения.
     */
    private ParentCommitInfo resolveDefaultParentInfo(RevWalk revWalk, RevCommit headCommit) throws IOException
    {
        int parentCount = headCommit.getParentCount();
        if (parentCount == 0)
        {
            return null;
        }

        if (parentCount == 1)
        {
            RevCommit parentCommit = revWalk.parseCommit(headCommit.getParent(0).getId());
            return new ParentCommitInfo(parentCommit.getId().getName(), false);
        }

        RevCommit mergedBranchParent = revWalk.parseCommit(headCommit.getParent(1).getId());
        return new ParentCommitInfo(mergedBranchParent.getId().getName(), true);
    }

    private record ParentCommitInfo(String commitId, boolean defaultFromMergedBranch) {
    }
}