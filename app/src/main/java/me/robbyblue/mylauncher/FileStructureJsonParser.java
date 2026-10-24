package me.robbyblue.mylauncher;

import android.content.Context;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import me.robbyblue.mylauncher.files.AppFile;
import me.robbyblue.mylauncher.files.FileNode;
import me.robbyblue.mylauncher.files.Folder;
import me.robbyblue.mylauncher.files.icons.IconData;
import me.robbyblue.mylauncher.widgets.WidgetElement;
import me.robbyblue.mylauncher.widgets.WidgetList;

/**
 * Parses the launcher's file structure from JSON into the folder map,
 * including the legacy widgetIds format, and cleans up the resulting
 * structure (folders linking to nonexistent children, orphaned folders).
 */
public class FileStructureJsonParser {

    private final Context context;

    public FileStructureJsonParser(Context context) {
        this.context = context;
    }

    public HashMap<String, Folder> parse(JSONObject jsonData) throws JSONException {
        HashMap<String, Folder> files = new HashMap<>();
        files.put("~", new Folder("~", "~"));

        Iterator<String> iterator = jsonData.keys();
        while (iterator.hasNext()) {
            String folderName = iterator.next();
            if (jsonData.get(folderName) instanceof JSONObject) {
                try {
                    JSONObject folderContentsJson = jsonData.getJSONObject(folderName);
                    files.put(folderName, parseFolder(folderContentsJson, folderName));
                } catch (Exception e) {

                }
            } else {
                try {
                    JSONArray folderContentsJson = jsonData.getJSONArray(folderName);
                    Folder folder = new Folder(folderName, folderName);
                    folder.getFiles().addAll(parseFilesList(folderContentsJson, folderName));
                    files.put(folderName, folder);
                } catch (Exception e) {

                }
            }
        }

        deleteBadFolders(files);
        clearOrphans(files);

        return files;
    }

    /**
     * folders that link to a child that doesn't exist as a key in the map
     * are removed from their parent's contents
     */
    private void deleteBadFolders(HashMap<String, Folder> files) {
        for (Folder folder : files.values()) {
            ArrayList<FileNode> folderFiles = folder.getFiles();
            Iterator<FileNode> iterator = folderFiles.iterator();
            while (iterator.hasNext()) {
                FileNode fileNode = iterator.next();
                if (!(fileNode instanceof Folder)) {
                    continue;
                }
                String filePath = ((Folder) fileNode).getFullPath();
                if (files.containsKey(filePath)) {
                    continue;
                }
                iterator.remove();
            }
        }
    }

    /**
     * removes folders unreachable from the root ("~")
     */
    private void clearOrphans(HashMap<String, Folder> files) {
        ArrayList<String> usedPaths = new ArrayList<>();
        Queue<String> uncheckedPaths = new LinkedList<>();

        usedPaths.add("~");
        uncheckedPaths.add("~");

        while (uncheckedPaths.size() > 0) {
            String testFullPath = uncheckedPaths.remove();
            Folder folder = files.get(testFullPath);
            if (folder == null) continue;
            for (FileNode file : folder.getFiles()) {
                if (!(file instanceof Folder)) continue;
                String fullPath = ((Folder) file).getFullPath();
                usedPaths.add(fullPath);
                uncheckedPaths.add(fullPath);
            }
        }

        files.keySet().removeIf(pathKey -> !usedPaths.contains(pathKey));
    }

    private Folder parseFolder(JSONObject folderContentsJson, String folderName) throws JSONException {
        Folder folder = new Folder(folderName, folderName);
        try {
            folder.getFiles().addAll(parseFilesList(folderContentsJson.getJSONArray("files"), folderName));
        } catch (Exception e) {

        }

        if (folderContentsJson.has("widgetIds")) {
            JSONArray widgetIds = folderContentsJson.getJSONArray("widgetIds");
            for (int i = 0; i < widgetIds.length(); i++) {
                WidgetElement widgetElement = new WidgetElement(widgetIds.getInt(i));
                folder.getWidgetList().addChild(widgetElement);
            }
        } else if (folderContentsJson.has("widgets")) {
            try {
                folder.setWidgetList(parseWidgetList(folderContentsJson.getJSONObject("widgets")));
            } catch (Exception e) {

            }
        }
        return folder;
    }

    private ArrayList<FileNode> parseFilesList(JSONArray folderContentsJson, String folderName) throws JSONException {
        ArrayList<String> folderNames = new ArrayList<>();
        ArrayList<FileNode> files = new ArrayList<>();
        for (int i = 0; i < folderContentsJson.length(); i++) {
            try {
                JSONObject fileNodeJson = folderContentsJson.getJSONObject(i);
                String name = fileNodeJson.getString("name");
                if (!FileNode.isValidName(name)) {
                    continue;
                }
                if (fileNodeJson.getString("type").equals("file")) {
                    String packageName = fileNodeJson.getString("package");

                    UserHandle user;

                    if (fileNodeJson.has("userHandleSerialNumber")) {
                        UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
                        user = manager.getUserForSerialNumber(fileNodeJson.getLong("userHandleSerialNumber"));
                    } else {
                        user = Process.myUserHandle();
                    }

                    IconData iconData = IconData.createIconDataFromJson(fileNodeJson);
                    files.add(new AppFile(name, packageName, iconData, user));
                } else {
                    if (folderNames.contains(name)) continue;
                    files.add(new Folder(name, folderName + "/" + name));
                    folderNames.add(name);
                }
            } catch (Exception e) {

            }
        }
        return files;
    }

    private WidgetList parseWidgetList(JSONObject jsonObject) throws JSONException {
        WidgetList widgetList = new WidgetList();

        widgetList.setSize(jsonObject.getDouble("size"));

        JSONArray childrenArray = jsonObject.getJSONArray("children");
        for (int i = 0; i < childrenArray.length(); i++) {
            try {
                JSONObject child = childrenArray.getJSONObject(i);

                String type = child.getString("type");

                if (type.equals("widget")) {
                    WidgetElement element = parseWidgetElement(child);
                    widgetList.addChild(element);
                } else if (type.equals("list")) {
                    WidgetList nestedList = parseWidgetList(child);
                    widgetList.addChild(nestedList);
                }
            } catch (Exception e) {

            }
        }

        return widgetList;
    }

    private WidgetElement parseWidgetElement(JSONObject jsonObject) throws JSONException {
        WidgetElement widgetElement = new WidgetElement(jsonObject.getInt("widgetId"));
        widgetElement.setSize(jsonObject.getDouble("size"));
        return widgetElement;
    }

}
