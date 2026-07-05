package com.sdp.edt.v8storagesync.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.sdp.edt.v8storagesync.preferences.messages"; //$NON-NLS-1$

    public static String V8StoragePreferencePage_Description;
    public static String V8StoragePreferencePage_UtilName;

    static
    {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
