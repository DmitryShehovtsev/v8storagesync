package com.sdp.edt.v8storagesync.ui.dialogs;

@FunctionalInterface
public interface ICommitHandler
{
    void onCommit(String hash, String message);
}