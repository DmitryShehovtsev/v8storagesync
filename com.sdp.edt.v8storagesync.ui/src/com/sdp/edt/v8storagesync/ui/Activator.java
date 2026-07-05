package com.sdp.edt.v8storagesync.ui;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.sdp.edt.v8storagesync.ui"; //$NON-NLS-1$
    public static final String PREF_UTIL_NAME = "utilName"; //$NON-NLS-1$

    private static Activator instance;
    private IPreferenceStore preferenceStore;

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        instance = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        instance = null;
        preferenceStore = null;
        super.stop(context);
    }

    @Override
    public IPreferenceStore getPreferenceStore()
    {
        if (preferenceStore == null)
        {
            preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, PLUGIN_ID);
            preferenceStore.setDefault(PREF_UTIL_NAME, "jeweltools"); //$NON-NLS-1$
        }
        return preferenceStore;
    }

    public static Activator getDefault()
    {
        return instance;
    }

    public String getUtilName()
    {
        return getPreferenceStore().getString(PREF_UTIL_NAME);
    }
}
