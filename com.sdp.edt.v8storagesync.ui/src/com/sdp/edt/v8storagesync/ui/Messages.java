package com.sdp.edt.v8storagesync.ui;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.sdp.edt.v8storagesync.ui.messages"; //$NON-NLS-1$

    public static String Plugin_Header;

    public static String AbstractActions_RepositoryMustNotBeNull;

    public static String Command_UnexpectedError;

    public static String PushDialog_Header;
    public static String PushDialog_hashLabel;
    public static String PushDialog_messageLabel;
    public static String PushDialog_check;
    public static String PushDialog_checkMessage;
    public static String PushDialog_Description;
    public static String PushDialog_StartError;
    public static String PushDialog_LoadCommitInfoError;

    public static String PushAction_Updated;
    public static String PushAction_NeedsRepeated;
    public static String PushAction_UnexpectedValue;
    public static String PushAction_ConfigDumpSuccess;
    public static String PushAction_ConfigDumpError;
    public static String PushAction_ApplicationNotFound;
    public static String PushAction_DumpingConf;
    public static String PushAction_BaseProjectNull;
    public static String PushAction_NoValidShellAvailable;
    public static String PushAction_HeadHashReadError;

    public static String ScriptRunnerJob_UserCancel;
    public static String ScriptRunnerJob_ErrorCode;
    public static String ScriptRunnerJob_Done;
    public static String ScriptRunnerJob_ErrorDuringExecution;
    public static String ScriptRunnerJob_HeaderPull;
    public static String ScriptRunnerJob_HeaderPush;
    public static String ScriptRunnerJob_CommandIsEmpty;
    public static String ScriptRunnerJob_CommandNotFound;
    public static String ScriptRunnerJob_CommandInvalidPath;
    public static String ScriptRunnerJob_CommandNotExecutable;
    public static String ScriptRunnerJob_ProjectRefreshError;
    public static String ScriptRunnerJob_ProjectTaskName;

    public static String GitActions_NoHeadCommit;
    public static String GitActions_HeadCommitInfo;
    public static String GitActions_ParentCommitInfo;
    public static String GitActions_NoParentCommit;
    public static String GitActions_RepositoryMustNotBeNull;

    public static String ConfigDumpService_UnsupportedProjectType;
    public static String ConfigDumpService_InfobaseNotFound;
    public static String ConfigDumpService_ExtensionNameNotFound;
    public static String ConfigDumpService_ConfigDumpManagerMustNotBeNull;

    public static String ConfigDumpManager_ProjectLocationNotFound;
    public static String ConfigDumpManager_DumpDirectoryCreateError;
    public static String ConfigDumpManager_DumpFileDeleteError;
    public static String ConfigDumpManager_DumpCleanupError;

    public static String HistoryViewUtils_FailedResolveGitRepo;
    public static String HistoryViewUtils_NotFoundProject;

    static
    {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}