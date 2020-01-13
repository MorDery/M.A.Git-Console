import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class Writer {
    public static String writeFolder(Folder i_Folder) {
        StringWriter sw = new StringWriter();
        i_Folder.getM_Items().forEach((k, v) ->
        {
            sw.write(v.getM_Name() + ";" + v.getM_Id() + ";" + v.getM_Type() + ";" + v.getLastUpdater() + ";" + v.getLastUpdateDate() + "\n");
        });
        return sw.toString();
    }

    public static String writeCommit(Commit i_Commit) {
        StringWriter sw = new StringWriter();
        try{
            sw.write(i_Commit.getM_RootFolderId() + ";" + i_Commit.getM_PrecedingCommitId().get(0) + ";" + i_Commit.getM_Message() + ";" + i_Commit.getM_Author() + ";" + i_Commit.getM_DateOfCreation() + "\n");
        }
        catch (Exception ex){ //if there's no precending commits - simply write an empty string
            sw.write(i_Commit.getM_RootFolderId() + ";" + "" + ";" + i_Commit.getM_Message() + ";" + i_Commit.getM_Author() + ";" + i_Commit.getM_DateOfCreation() + "\n");
        }
        return sw.toString();
    }

    public static String writeBlob(Blob i_Blob) {
        StringWriter sw = new StringWriter();
        sw.write(i_Blob.getM_Content());

        return sw.toString();
    }

    public static void writeBlob (Path i_Path, Blob i_Blob){
        try {
            Files.write(i_Path, i_Blob.getM_Content().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void WriteToTextFile(Path textFilePath,String i_ContentToAdd) throws IOException {
        Files.write(textFilePath,i_ContentToAdd.getBytes());
    }

}
