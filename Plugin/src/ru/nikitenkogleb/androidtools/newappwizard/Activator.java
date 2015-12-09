package ru.nikitenkogleb.androidtools.newappwizard;

import java.util.Locale;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	
	/**	Default locale. */
	private static final Locale DEFAULT_LOCALE = Locale.getDefault();
	
	/**	Icons folder string format */
	private static final String FORMAT_ICONS_PATH = "icons/%s";

	// The plug-in ID
	public static final String PLUGIN_ID =
			"ru.nikitenkogleb.androidtools.newappwizard"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative name
	 *
	 * @param name the name
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String name) {
		return imageDescriptorFromPlugin(PLUGIN_ID,
				String.format(DEFAULT_LOCALE, FORMAT_ICONS_PATH, name));
	}
}
