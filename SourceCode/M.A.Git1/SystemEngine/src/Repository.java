import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Repository {

    private String m_Location;
    private String m_Name = null;
    private final Map<String,Blob> m_Blobs = new HashMap<>();
    private final Map<String,Folder> m_Folders = new HashMap<>();
    private final Map<String,Commit> m_Commits = new HashMap<>();
    private final Map<String,Branch> m_Branches = new HashMap<>();


    public Repository(String i_Location,String i_Name) {
        m_Location = i_Location;
        m_Name = i_Name;
    }
    public Repository(String i_Location) {
        m_Location = i_Location;
    }

    public String getM_Location() {
        return m_Location;
    }

    public  Map<String,Blob>  getM_Blobs() {
        return m_Blobs;
    }

    public  Map<String,Folder>  getM_Folders() {
        return m_Folders;
    }

    public Map<String, Commit> getM_Commits() {
        return m_Commits;
    }

    public Map<String, Branch> getM_Branches() {
        return m_Branches;
    }

    public String getHeadBranchFromRepository(){
        return null ;
    }

    public String getM_Name() {
        return m_Name;
    }

    public void setM_Name(String i_Name){
        m_Name = i_Name;
    }
}
