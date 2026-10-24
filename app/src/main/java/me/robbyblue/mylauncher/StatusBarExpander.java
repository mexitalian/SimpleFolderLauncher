package me.robbyblue.mylauncher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

import java.lang.reflect.Method;

/**
 * Expands the notification or quick settings panel.
 * This uses reflection because the StatusBarManager methods
 * for expanding the panels are hidden from the public API.
 */
public class StatusBarExpander {

    public static void expandPanel(Context context, boolean settingsPanel) {
        String panelMethod = settingsPanel
                ? "expandSettingsPanel"
                : "expandNotificationsPanel";

        // as per https://stackoverflow.com/questions/31897920/how-to-open-the-android-quick-notification-setting
        try {
            @SuppressLint("WrongConstant") Object service = context.getSystemService("statusbar");
            Class<?> statusBarManager = Class.forName("android.app.StatusBarManager");

            Method expand = statusBarManager.getMethod(panelMethod);
            expand.invoke(service);
        } catch (Exception e) {
            String toastText = "couldn't open quick settings: " + e;
            Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
            toast.show();
            e.printStackTrace();
        }
    }

}
