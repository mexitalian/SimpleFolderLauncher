package me.robbyblue.mylauncher;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.os.UserHandle;

/**
 * Keeps the app cache up to date when packages are
 * added, changed or removed on the device.
 */
public class LauncherAppsWatcher {

    public static void register(Context context, AppsListCache appCache) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        launcherApps.registerCallback(new LauncherApps.Callback() {
            @Override
            public void onPackageAdded(String packageName, UserHandle user) {
                if (appCache == null) return;
                appCache.indexPackage(packageName, context);
            }

            @Override
            public void onPackageChanged(String packageName, UserHandle user) {
                if (appCache == null) return;
                appCache.indexPackage(packageName, context);
            }

            @Override
            public void onPackageRemoved(String packageName, UserHandle user) {
                if (appCache == null) return;
                appCache.removePackage(packageName);
            }

            @Override
            public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            }

            @Override
            public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
            }
        });
    }

}
