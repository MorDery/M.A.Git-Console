import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Creator {
    public static void createWC(MAGit i_MyMAGit) {
        String folderSHA = null;
        String commitSHA = null;
        Map<String, Folder.Item> items = null;
        String pathWC = "WC";

        Path wcPath = Paths.get(i_MyMAGit.getM_Repositories().getM_Location() + "/" + pathWC);
        try {
            Files.createDirectories(wcPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, Branch> branch : i_MyMAGit.getM_Repositories().getM_Branches().entrySet()) {
            Branch v = branch.getValue();
            if (v.isM_IsHead()) {
                commitSHA = v.getM_PointedCommitId();
                break;
            }
        }
        if (commitSHA != null && !(commitSHA.equals(""))) {
            folderSHA = i_MyMAGit.getM_Repositories().getM_Commits().get(commitSHA).getM_RootFolderId();
        }

        if (folderSHA != null ) {
            items = i_MyMAGit.getM_Repositories().getM_Folders().get(folderSHA).getM_Items();
            if (items != null) {
                createItemsInWC(i_MyMAGit, items, wcPath );
            }
        }
    }

    public static void createItemsInWC(MAGit i_MyMAGit, Map<String, Folder.Item> i_MyItems, Path i_Path) {
        for (Map.Entry<String, Folder.Item> entry : i_MyItems.entrySet()) {
            Folder.Item v = entry.getValue();
            if (v.getM_Type() == Folder.Item.eItemType.BLOB) {
                findBlob(v.getM_Id(), i_MyMAGit.getM_Repositories().getM_Blobs(), Paths.get(i_Path + "/" + v.getM_Name()));
            } else {
                findFolder(i_MyMAGit, v.getM_Id(), i_MyMAGit.getM_Repositories().getM_Folders(), Paths.get(i_Path + "/" + v.getM_Name()));
            }
        }
    }

    public static void findBlob (String i_Id, Map < String, Blob > i_Blobs, Path i_Path ){
        Writer.writeBlob(i_Path, i_Blobs.get(i_Id));
    }

    public static void findFolder (MAGit i_MyMAGit,String i_Id, Map < String, Folder > i_Folders, Path i_Path ) {
        try {
            Files.createDirectories(i_Path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        createItemsInWC(i_MyMAGit, i_Folders.get(i_Id).getM_Items(), i_Path);
    }

    public static List<String> getFileNamesOfFilesInAFolder(String i_FolderFullPath) {
        List<String> result = new ArrayList<String>();

        File[] files = new File(i_FolderFullPath).listFiles();

        for (File file : files) {
            if (file.isFile()) {
                result.add(file.getName());
            }
        }

        return result;
    }

    public static Commit createCommitFromSplittedCOntent(String [] commitContent)
    {
        String rootFolderId = commitContent[0];
        String precedingCommitId = commitContent[1];
        String message = commitContent[2];
        String author = commitContent[3];
        String dateOfCreation = commitContent[4].trim();
        Commit commit = new Commit(rootFolderId,message,author,dateOfCreation,precedingCommitId);
        return commit;
    }
}
