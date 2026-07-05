package com.sdp.edt.v8storagesync.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.sdp.edt.v8storagesync.ui.Activator;

public class V8StoragePreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage
{

    private static final String GITHUB_URL = "https://github.com/DmitryShehovtsev/v8storagesync"; //$NON-NLS-1$

    public V8StoragePreferencePage()
    {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription(Messages.V8StoragePreferencePage_Description);
    }

    @Override
    public void init(IWorkbench workbench)
    {
        //
    }

    @Override
    public void createFieldEditors()
    {
        Composite parent = getFieldEditorParent();

        createGitHubLink(parent);

        addSpacer(parent);

        StringFieldEditor fieldUtilName =
            new StringFieldEditor(Activator.PREF_UTIL_NAME, Messages.V8StoragePreferencePage_UtilName,
                getFieldEditorParent());
        fieldUtilName.setEmptyStringAllowed(false);
        addField(fieldUtilName);

    }

    private void createGitHubLink(Composite parent)
    {
        Link link = new Link(parent, SWT.NONE);
        link.setText("<a>" + GITHUB_URL + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
        link.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Program.launch(GITHUB_URL);
            }
        });

        GridData linkData = new GridData(GridData.FILL_HORIZONTAL);
        linkData.horizontalSpan = 3;
        link.setLayoutData(linkData);
    }

    private void addSpacer(Composite parent)
    {
        Label spacer = new Label(parent, SWT.NONE);
        GridData spacerData = new GridData();
        spacerData.horizontalSpan = 3;
        spacerData.heightHint = 15;
        spacer.setLayoutData(spacerData);
    }
}
