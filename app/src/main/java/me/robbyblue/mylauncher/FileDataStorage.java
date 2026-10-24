package me.robbyblue.mylauncher;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import me.robbyblue.mylauncher.files.AppFile;
import me.robbyblue.mylauncher.files.FileNode;
import me.robbyblue.mylauncher.files.Folder;

/**
 * Holds the launcher's folder structure and provides tree operations
 * (create/rename/move/remove). JSON parsing lives in
 * {@link FileStructureJsonParser}, serialization in
 * {@link FileStructureJsonSerializer}.
 */
public class FileDataStorage {

    private Context context;
    private static FileDataStorage instance;
    File structureFile;

    HashMap<String, Folder> files;

    private FileDataStorage() {
    }

    private FileDataStorage(File structureFile, Context context) {
        this.structureFile = structureFile;
        this.context = context;
        if (this.structureFile != null && this.structureFile.exists()) {
            try {
                this.loadFromInputStream(new FileInputStream(this.structureFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            this.files = createEmptyFolderStructure();
        }
    }

    private FileDataStorage(InputStream inputStream, Context context) {
        this.structureFile = null;
        this.context = context;
        this.loadFromInputStream(inputStream);
    }

    public static FileDataStorage getInstanceAssumeExists() {
        return instance;
    }

    public static FileDataStorage getInstanceOrCreate() {
        if (instance == null) {
            instance = new FileDataStorage();
        }
        return instance;
    }

    public static FileDataStorage getInstance() throws NotInitializedException {
        if (instance == null) {
            throw new NotInitializedException("AppListCache not loaded");
        }
        return instance;
    }

    public static FileDataStorage getInstance(File file, Context context) {
        if (instance == null) {
            instance = new FileDataStorage(file, context);
        }
        return instance;
    }

    public static FileDataStorage getNewInstance(InputStream inputStream, Context context) {
        return new FileDataStorage(inputStream, context);
    }

    private FileStructureJsonParser getParser() {
        return new FileStructureJsonParser(context);
    }

    private FileStructureJsonSerializer getSerializer() {
        return new FileStructureJsonSerializer(context);
    }

    // ---- loading / saving ----

    public void loadFromInputStream(InputStream inputStream) {
        try {
            String fileContents = readInputStream(inputStream);
            JSONObject jsonData = new JSONObject(fileContents);
            loadFromJson(jsonData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void loadFromJson(JSONObject jsonData) {
        try {
            this.files = getParser().parse(jsonData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void storeFilesStructure() {
        try {
            JSONObject jsonData = getSerializer().toJson(files);
            writeFile(structureFile, jsonData.toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject getFileStructureAsJson() {
        return getSerializer().toJson(files);
    }

    public String getDataAsJsonString() {
        try {
            JSONObject data = getSerializer().toJson(files);
            return getSerializer().removeWidgets(data).toString(2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void importFromInputStream(InputStream is) {
        try {
            String contents = readInputStream(is);
            JSONObject data = new JSONObject(contents);

            data = getSerializer().removeWidgets(data);

            loadFromJson(data);
            storeFilesStructure();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- tree operations ----

    public void createFile(String parentFolder, AppFile appFile) {
        ArrayList<FileNode> parentFolderContents = getFolderContents(parentFolder).getFiles();
        parentFolderContents.add(appFile);
        storeFilesStructure();
    }

    public void createFolder(String parentFolder, String name) {
        ArrayList<FileNode> parentFolderContents = getFolderContents(parentFolder).getFiles();
        String fullPath = parentFolder + "/" + name;
        if (files.containsKey(fullPath)) return;
        parentFolderContents.add(new Folder(name, fullPath));
        files.put(fullPath, new Folder(name, fullPath));
        storeFilesStructure();
    }

    public void renameFile(String parentFolder, int fileIndex, String newName) {
        ArrayList<FileNode> contents = getFolderContents(parentFolder).getFiles();
        FileNode item = contents.get(fileIndex);
        if (item instanceof Folder) {
            String fullPath = parentFolder + "/" + newName;
            if (files.containsKey(fullPath)) return;

            // fix full path of folder and its subfolders
            Folder folder = ((Folder) item);
            String oldPath = folder.getFullPath();
            String newPath = oldPath.substring(0, oldPath.lastIndexOf("/") + 1) + newName;

            updateFolderPath(folder, oldPath, newPath);
        }
        item.setName(newName);
        storeFilesStructure();
    }

    private void updateFolderPath(Folder folder, String oldPath, String newPath) {
        String subfolderPath = folder.getFullPath();
        boolean shouldRename = subfolderPath.equals(oldPath) || subfolderPath.startsWith(oldPath + "/");
        if (!shouldRename)
            return;

        Folder folderContents = getFolderContents(folder);
        for (FileNode fileNode : folderContents.getFiles()) {
            if (!(fileNode instanceof Folder))
                continue;
            Folder subfolder = (Folder) fileNode;
            updateFolderPath(subfolder, oldPath, newPath);
        }

        folder.setFullPath(folder.getFullPath().replace(oldPath, newPath));
        folderContents.setFullPath(folderContents.getFullPath().replace(oldPath, newPath));

        files.put(folder.getFullPath(), folderContents);
        files.remove(subfolderPath);
    }

    public void removeFile(String parentFolder, int fileIndex) {
        ArrayList<FileNode> contents = getFolderContents(parentFolder).getFiles();
        FileNode item = contents.get(fileIndex);
        if (item instanceof Folder) {
            removeFolder(((Folder) item).getFullPath());
        }
        contents.remove(fileIndex);
        storeFilesStructure();
    }

    private void removeFolder(String path) {
        Folder folder = getFolderContents(path);
        for (FileNode file : folder.getFiles()) {
            if (!(file instanceof Folder)) continue;
            removeFolder(((Folder) file).getFullPath());
        }
        files.remove((folder).getFullPath());
    }

    public void moveFile(String parentFolder, int initialIndex, int moveIndex) {
        ArrayList<FileNode> contents = getFolderContents(parentFolder).getFiles();
        if (moveIndex < 0 || moveIndex >= contents.size()) return;
        FileNode item = contents.get(initialIndex);
        contents.remove(initialIndex);
        contents.add(moveIndex, item);
        storeFilesStructure();
    }

    public ArrayList<Folder> getFolders() {
        ArrayList<Folder> folders = new ArrayList<>();
        for (String folderPath : files.keySet()) {
            String[] parts = folderPath.split("/");
            String folderName = parts[parts.length - 1];
            folders.add(new Folder(folderName, folderPath));
        }
        return folders;
    }

    public Folder getFolderContents(Folder folder) {
        return getFolderContents(folder.getFullPath());
    }

    public Folder getFolderContents(String path) {
        if (path.equals("~") && !files.containsKey(path)) {
            files.put("~", new Folder("~", "~"));
        }
        if (files.containsKey(path)) return files.get(path);
        return null;
    }

    // ---- helpers ----

    private HashMap<String, Folder> createEmptyFolderStructure() {
        HashMap<String, Folder> files = new HashMap<>();
        files.put("~", new Folder("~", "~"));
        return files;
    }

    private String readInputStream(InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder res = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                res.append(line);
            }

            return res.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeFile(File file, String content) {
        if (file == null) return;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
