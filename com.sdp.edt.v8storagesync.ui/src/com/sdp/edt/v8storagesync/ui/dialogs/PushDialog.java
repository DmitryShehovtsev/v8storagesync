package com.sdp.edt.v8storagesync.ui.dialogs;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.sdp.edt.v8storagesync.git.GitActions;
import com.sdp.edt.v8storagesync.support.LogHelper;
import com.sdp.edt.v8storagesync.support.V8StorageSyncLock;
import com.sdp.edt.v8storagesync.ui.Messages;

public class PushDialog
    extends Dialog
{
    private static final int DIALOG_WIDTH = 420;
    private static final int DIALOG_HEIGHT = 400;
    private static final int SPACER_HEIGHT = 10;

    private static Shell currentDialogShell;

    private final ICommitHandler handler;
    private final GitActions gitActions;

    private Text hashCommitText;
    private Text messageCommitText;
    private Text commitInfoText;

    private boolean submitted;
    private boolean headCommitAvailable = true;

    private PushDialog(Repository repo, Shell parentShell, ICommitHandler handler)
    {
        super(parentShell);
        this.handler = handler;
        this.gitActions = new GitActions(repo);
    }

    public static void show(Repository repo, Shell parentShell, ICommitHandler handler)
    {
        if (currentDialogShell != null && !currentDialogShell.isDisposed())
        {
            currentDialogShell.setActive();
            currentDialogShell.forceFocus();
            return;
        }

        PushDialog dialog = new PushDialog(repo, parentShell, handler);
        dialog.setBlockOnOpen(false);
        dialog.open();

        currentDialogShell = dialog.getShell();
    }

    @Override
    public boolean close()
    {
        try
        {
            if (!submitted)
            {
                V8StorageSyncLock.release();
            }
            return super.close();
        }
        finally
        {
            currentDialogShell = null;
        }
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(Messages.PushDialog_Header);
        newShell.setSize(DIALOG_WIDTH, DIALOG_HEIGHT);

        Shell parent = getParentShell();
        if (parent != null)
        {
            Rectangle parentBounds = parent.getBounds();
            Point dialogSize = newShell.getSize();
            int x = parentBounds.x + (parentBounds.width - dialogSize.x) / 2;
            int y = parentBounds.y + (parentBounds.height - dialogSize.y) / 2;
            newShell.setLocation(x, y);
        }
    }

    @Override
    protected int getShellStyle()
    {
        return SWT.MODELESS | SWT.RESIZE | SWT.TITLE | SWT.BORDER;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = (Composite)super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = SPACER_HEIGHT;
        layout.marginWidth = SPACER_HEIGHT;
        container.setLayout(layout);

        Label hintLabel = new Label(container, SWT.WRAP);
        hintLabel.setText(Messages.PushDialog_Description);
        hintLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
        hintLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        addSpacer(container);

        commitInfoText = new Text(container, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
        commitInfoText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        addSpacer(container);

        Label hashLabel = new Label(container, SWT.NONE);
        hashLabel.setText(Messages.PushDialog_hashLabel);
        hashLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        hashCommitText = new Text(container, SWT.BORDER | SWT.SINGLE);
        hashCommitText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        hashCommitText.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
                {
                    okPressed();
                }
            }
        });

        Label messageLabel = new Label(container, SWT.NONE);
        messageLabel.setText(Messages.PushDialog_messageLabel);
        messageLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        messageCommitText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData messageData = new GridData(SWT.FILL, SWT.FILL, true, true);
        messageCommitText.setLayoutData(messageData);

        initializeCommitData();

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        updateOkButtonState();
    }

    @Override
    protected void okPressed()
    {
        if (!headCommitAvailable)
        {
            MessageDialog.openError(getShell(), Messages.Plugin_Header, Messages.GitActions_NoHeadCommit);
            return;
        }

        String message = messageCommitText.getText();
        if (message == null || message.trim().isEmpty())
        {
            MessageDialog.openError(getShell(), Messages.PushDialog_check, Messages.PushDialog_checkMessage);
            return;
        }

        String hashValue = hashCommitText.getText().trim();

        try
        {
            if (handler != null)
            {
                handler.onCommit(hashValue, message);
            }

            submitted = true;
            super.okPressed();
        }
        catch (RuntimeException e)
        {
            LogHelper.logError(Messages.PushDialog_StartError, e);
            MessageDialog.openError(getShell(), Messages.Plugin_Header, Messages.PushDialog_StartError);
        }
    }

    @Override
    protected void cancelPressed()
    {
        super.cancelPressed();
    }

    private void initializeCommitData()
    {
        try
        {
            headCommitAvailable = gitActions.hasHeadCommit();

            if (!headCommitAvailable)
            {
                commitInfoText.setText(Messages.GitActions_NoHeadCommit);
                hashCommitText.setText(""); //$NON-NLS-1$
                messageCommitText.setText(""); //$NON-NLS-1$
                messageCommitText.setFocus();
                updateOkButtonState();
                return;
            }

            commitInfoText.setText(gitActions.getCommitContextInfo());
            hashCommitText.setText(gitActions.getParentHash());
            messageCommitText.setText(""); //$NON-NLS-1$
            messageCommitText.setFocus();
            updateOkButtonState();
        }
        catch (InvocationTargetException e)
        {
            headCommitAvailable = false;
            LogHelper.logError(Messages.PushDialog_LoadCommitInfoError, e);
            commitInfoText.setText(Messages.PushDialog_LoadCommitInfoError);
            hashCommitText.setText(""); //$NON-NLS-1$
            messageCommitText.setText(""); //$NON-NLS-1$
            messageCommitText.setFocus();
            updateOkButtonState();
        }
    }

    private void updateOkButtonState()
    {
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null && !okButton.isDisposed())
        {
            okButton.setEnabled(headCommitAvailable);
        }
    }

    private void addSpacer(Composite container)
    {
        Label spacer = new Label(container, SWT.NONE);
        GridData spacerData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        spacerData.heightHint = SPACER_HEIGHT;
        spacer.setLayoutData(spacerData);
    }
}