package me.robbyblue.mylauncher;

import android.content.Context;
import android.os.UserManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import me.robbyblue.mylauncher.files.AppFile;
import me.robbyblue.mylauncher.files.FileNode;
import me.robbyblue.mylauncher.files.Folder;
import me.robbyblue.mylauncher.widgets.WidgetElement;
import me.robbyblue.mylauncher.widgets.WidgetLayout;
import me.robbyblue.mylauncher.widgets.WidgetList;

/**
 * Serializes the launcher's folder structure to JSON.
 */
public class FileStructureJsonSerializer {

    private final Context context;

    public FileStructureJsonSerializer(Context context) {
        this.context = context;
    }

    public JSONObject toJson(HashMap<String, Folder> files) {
        try {
            JSONObject jsonData = new JSONObject();
            for (String folderName : files.keySet()) {
                jsonData.put(folderName, jsonifyFolder(files.get(folderName)));
            }
            return jsonData;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JSONObject jsonifyFolder(Folder folder) throws JSONException {
        JSONObject folderObject = new JSONObject();
        JSONArray filesArray = new JSONArray();
        for (FileNode fileNode : folder.getFiles()) {
            JSONObject fileJson = new JSONObject();
            fileJson.put("name", fileNode.getName());
            fileJson.put("icon", fileNode.getIconData().toJson());
            if (fileNode instanceof AppFile) {
                UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
                long serialNumber = manager.getSerialNumberForUser(((AppFile) fileNode).getUser());

                fileJson.put("type", "file");
                fileJson.put("package", ((AppFile) fileNode).getPackageName());
                fileJson.put("userHandleSerialNumber", serialNumber);
            } else if (fileNode instanceof Folder) {
                fileJson.put("type", "folder");
            }
            filesArray.put(fileJson);
        }
        folderObject.put("files", filesArray);

        try {
            JSONObject widgetsJson = jsonifyWidgetList(folder.getWidgetList());
            folderObject.put("widgets", widgetsJson);
        } catch (Exception e) {

        }

        return folderObject;
    }

    private JSONObject jsonifyWidgetList(WidgetList widgetList) throws JSONException {
        JSONArray childrenArray = new JSONArray();
        for (WidgetLayout widgetLayout : widgetList.getChildren()) {
            if (widgetLayout instanceof WidgetElement) {
                try {
                    childrenArray.put(jsonifyWidget((WidgetElement) widgetLayout));
                } catch (Exception e) {

                }
            }
            if (widgetLayout instanceof WidgetList) {
                try {
                    childrenArray.put(jsonifyWidgetList((WidgetList) widgetLayout));
                } catch (Exception e) {

                }
            }
        }

        JSONObject widgetObject = new JSONObject();
        widgetObject.put("type", "list");
        widgetObject.put("size", widgetList.getSize());
        widgetObject.put("children", childrenArray);

        return widgetObject;
    }

    private JSONObject jsonifyWidget(WidgetElement widgetElement) throws JSONException {
        JSONObject widgetObject = new JSONObject();
        widgetObject.put("type", "widget");
        widgetObject.put("size", widgetElement.getSize());
        widgetObject.put("widgetId", widgetElement.getAppWidgetId());

        return widgetObject;
    }

    /**
     * replaces each folder's widgets with an empty object,
     * used for export/import so widget ids don't transfer between devices
     */
    public JSONObject removeWidgets(JSONObject data) {
        try {
            for (Iterator<String> it = data.keys(); it.hasNext(); ) {
                String key = it.next();
                data.getJSONObject(key).put("widgets", new JSONObject());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return data;
    }

}
