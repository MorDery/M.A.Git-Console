import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;


public class MAGit {

    private String m_ActiveUserName;
    private Repository m_CurrentRepository;
    private SaverToXml m_SaveRepositoryToXML = null;

    public MAGit(String m_ActiveUserName, Repository i_CurrentRep) {
        this.m_ActiveUserName = m_ActiveUserName;
        m_CurrentRepository = i_CurrentRep;
    }

    public MAGit(String m_ActiveUserName) {
        this.m_ActiveUserName = m_ActiveUserName;
        m_CurrentRepository = null;
    }

    public String getM_ActiveUserName() {
        return m_ActiveUserName;
    }

    public void setM_ActiveUserName(String m_ActiveUserName) {
        this.m_ActiveUserName = m_ActiveUserName;
    }

    public Repository getM_Repositories() {
        return m_CurrentRepository;
    }

    public void setM_Repositories(Repository m_Repositories) {
        this.m_CurrentRepository = m_Repositories;
    }

    public void spanHeadBranchToOurObjects(String i_RepositoryFullPath) throws FileNotFoundException, IOException {

        File file = new File(i_RepositoryFullPath + "/.magit/branches/Head.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String activeBranchName = br.readLine();

        file = new File(i_RepositoryFullPath + "/.magit/branches/" + activeBranchName);
        br = new BufferedReader(new FileReader(file));
        String pointedCommitId = br.readLine();

        if (m_CurrentRepository != null) {
            clearRepository();
        }
        m_CurrentRepository = new Repository(i_RepositoryFullPath);


        m_CurrentRepository.getM_Branches().put(activeBranchName, new Branch(activeBranchName, pointedCommitId, true));
        if(pointedCommitId != null) {
            spanCommitToOurObjects(i_RepositoryFullPath, pointedCommitId);
        }

        String nameRepository = extractRepositoryNameFromPath(i_RepositoryFullPath);
        this.getM_Repositories().setM_Name(nameRepository);
    }

    public void clearRepository() {

        m_CurrentRepository.getM_Blobs().clear();
        m_CurrentRepository.getM_Folders().clear();
        m_CurrentRepository.getM_Commits().clear();
        m_CurrentRepository.getM_Branches().clear();
    }

    private void spanCommitToOurObjects(String i_RepositoryFullPath, String i_CommitId) throws FileNotFoundException, IOException {

        String commitContent = FileZipper.unZip(i_RepositoryFullPath + "/.magit/objects/" + i_CommitId);
        String[] splittedCommitContent = commitContent.split(";");
        String rootFolderId = splittedCommitContent[0];
        String precedingCommitId = splittedCommitContent[1];
        String message = splittedCommitContent[2];
        String author = splittedCommitContent[3];
        String dateOfCreation = splittedCommitContent[4].trim();

        m_CurrentRepository.getM_Commits().put(i_CommitId, new Commit(rootFolderId, message, author, dateOfCreation, precedingCommitId));

        m_CurrentRepository.getM_Folders().put(rootFolderId, new Folder(true));

        spanFolderToOurObjectsRec(m_CurrentRepository.getM_Folders().get(rootFolderId), i_RepositoryFullPath, rootFolderId);
    }

    private void spanFolderToOurObjectsRec(Folder i_currentFolder, String i_RepositoryFullPath, String i_FolderId) throws IOException {

        String folderContent = FileZipper.unZip(i_RepositoryFullPath + "/.magit/objects/" + i_FolderId);
        String[] lines = folderContent.split("\n");
        // add items to list
        for (int i = 0; i < lines.length-1; i++) {

            String[] splittedItemInFolderContent = lines[i].split(";");
            String name = splittedItemInFolderContent[0];
            String id = splittedItemInFolderContent[1];
            String type = splittedItemInFolderContent[2];
            String lastUpdater = splittedItemInFolderContent[3];
            String lastUpdateDate = splittedItemInFolderContent[4].trim();
            i_currentFolder.getM_Items().put(id, new Folder.Item(name, id, Folder.Item.eItemType.valueOf(type.toUpperCase()), lastUpdater, lastUpdateDate));
            if(type.equals("Folder")) {
                spanFolderToOurObjectsRec(i_currentFolder,i_RepositoryFullPath + "/.magit/objects/"+id,id);
            }
        }


        for (Folder.Item itemData : i_currentFolder.getM_Items().values()) {
            if (itemData.getM_Type() == Folder.Item.eItemType.FOLDER) {
                Folder nextFolder = new Folder(false);
                m_CurrentRepository.getM_Folders().put(itemData.getM_Id(), nextFolder);
                spanFolderToOurObjectsRec(nextFolder, i_RepositoryFullPath, itemData.getM_Id());
            } else {
                spanBlobToOurObjects(i_RepositoryFullPath, itemData.getM_Id());
            }

        }
    }

    private void spanBlobToOurObjects(String i_RepositoryFullPath, String i_BlobId) throws IOException {
        String blobContent = FileZipper.unZip(i_RepositoryFullPath + "/.magit/objects/" + i_BlobId).trim();
        m_CurrentRepository.getM_Blobs().put(i_BlobId, new Blob(blobContent));
    }

    public boolean isRepositoryExists(String i_RepositoryFullPath) {
        File file = new File(i_RepositoryFullPath + "/.magit");
        return file.exists();
    }

    private void spanCommitToZip(String i_RepositoryFullPath, String i_CommitId) {
        FileZipper.WriteToZip(i_RepositoryFullPath, i_CommitId, i_RepositoryFullPath);
    }

    public void ShowCurrentCommitFilesFromRepository(List<String> i_ItemsToDisplay) throws IOException {
        String folderSHA = null;
        String commitSHA = null;
        Map<String, Folder.Item> items = null;
        Path generalPath = Paths.get(this.getM_Repositories().getM_Location());

        String headBranch = getHeadBranchFromRepository();
        commitSHA = m_CurrentRepository.getM_Branches().get(headBranch).getM_PointedCommitId();

        if (commitSHA != null && !(commitSHA.equals(""))) {
            folderSHA = this.getM_Repositories().getM_Commits().get(commitSHA).getM_RootFolderId();
        }

        if (folderSHA != null) {
            generalPath = Paths.get(generalPath + "/WC");
            items = this.getM_Repositories().getM_Folders().get(folderSHA).getM_Items();
            if (items.size() != 0) {
                showItemsFromCommit(items, generalPath,i_ItemsToDisplay);
            }
        }
    }

    private void showItemsFromCommit(Map<String, Folder.Item> i_MyItems, Path i_FullPath,List<String> i_ItemsToDisplay) {

        for (Map.Entry<String, Folder.Item> entry : i_MyItems.entrySet()) {
            Folder.Item v = entry.getValue();
            if (v.getM_Type() == Folder.Item.eItemType.BLOB) {
                finAndPrintBlob(v, Paths.get(i_FullPath + "/" + v.getM_Name()),i_ItemsToDisplay);
            }
            else {
                findFolder(v, Paths.get(i_FullPath + "/" + v.getM_Name()),i_ItemsToDisplay);
            }
        }
    }

    private void finAndPrintBlob(Folder.Item i_BloblItem, Path i_FullPath,List<String> i_ItemsToDisplay) {
        Blob blobToFind = this.getM_Repositories().getM_Blobs().get(i_BloblItem.getM_Id());

            if (blobToFind != null) {
                i_ItemsToDisplay.add(i_FullPath.toString());
                i_ItemsToDisplay.add("Blob");
                i_ItemsToDisplay.add(i_BloblItem.getM_Id());
                i_ItemsToDisplay.add(i_BloblItem.getLastUpdater());
                i_ItemsToDisplay.add(i_BloblItem.getLastUpdateDate());
            }
        }

    private void findFolder(Folder.Item i_FolderItem, Path i_FullPath,List<String> i_ItemsToDisplay) {
        Folder folderToFind = this.getM_Repositories().getM_Folders().get(i_FolderItem.getM_Id());

        if (folderToFind != null) {
            i_ItemsToDisplay.add(i_FullPath.toString());
            i_ItemsToDisplay.add("Folder");
            i_ItemsToDisplay.add(i_FolderItem.getM_Id());
            i_ItemsToDisplay.add(i_FolderItem.getLastUpdater());
            i_ItemsToDisplay.add(i_FolderItem.getLastUpdateDate());
            showItemsFromCommit(folderToFind.getM_Items(), i_FullPath, i_ItemsToDisplay);
        }
    }

    private String getHeadBranchFromRepository() throws IOException {
        File file = new File(m_CurrentRepository.getM_Location() + "/.magit/branches/Head.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String activeBranchName = br.readLine();
        file = null ;
        br.close();
        return activeBranchName;
    }

    public void readAllBranches(List<String> i_BranchesToPrint) throws IOException,NullPointerException {
        String noCommitMessge = "no commit in the brunch";
        String pointedCommitSha1 = null;
        BufferedReader br = null;
        String pathBranches = (this.getM_Repositories().getM_Location() + "/.magit/branches");
        File folder = new File(pathBranches);
        File headFolder = new File(this.getM_Repositories().getM_Location() + "/.magit/branches/Head.txt");
        br = new BufferedReader(new FileReader(headFolder));
        String activeBranchName = br.readLine();
        br.close();
        if(activeBranchName == null)
        {
            throw new NullPointerException("No branches available on this repository!");
        }

        for (File fileEntry : folder.listFiles()) {
            if (!(fileEntry.getName().toLowerCase().equals("head.txt"))) {
                if (activeBranchName.equals(fileEntry.getName())) {
                    i_BranchesToPrint.add("That's the head active branch: ");
                }
                br = new BufferedReader(new FileReader(fileEntry));
                i_BranchesToPrint.add(fileEntry.getName());
                pointedCommitSha1 = br.readLine();
                if (pointedCommitSha1 != null && !(pointedCommitSha1.equals(""))) {
                    br.close();
                    i_BranchesToPrint.add(pointedCommitSha1);
                    String commitContent = FileZipper.unZip(this.getM_Repositories().getM_Location() + "/.magit/objects/" + pointedCommitSha1);
                    String[] splittedCommitContent = commitContent.split(";");
                    String message = splittedCommitContent[2];
                    i_BranchesToPrint.add(message);
                }
                else{
                    br.close();
                    i_BranchesToPrint.add(noCommitMessge);
                }
            }
        }
    }

    public void createBrunch(String i_NewBranchToAdd) throws IOException,NullPointerException {
        String commitSHA = null;
        String brunchName = i_NewBranchToAdd;
        commitSHA = m_CurrentRepository.getM_Branches().get(getHeadBranchFromRepository()).getM_PointedCommitId();
        if (commitSHA != null && !(commitSHA.equals(""))) {
            Branch newBranch = new Branch(brunchName, commitSHA, false);
            m_CurrentRepository.getM_Branches().put(brunchName, newBranch);
            Files.write(Paths.get(this.getM_Repositories().getM_Location() + "/.magit/branches/" + brunchName), newBranch.getM_PointedCommitId().getBytes());
        }
        else {
            throw new NullPointerException("No commits in the repository! please try again!");
        }
    }

    public void deleteBrunch(String i_BranchToDelete) throws IOException,NullPointerException {

        if(getM_Repositories().getM_Branches() == null)
        {
            throw new NullPointerException("No branches available on this repository!");
        }

        File folder = new File(this.getM_Repositories().getM_Location() + "/.magit/branches/" + i_BranchToDelete);
        if(folder.exists()) {
            if(isHeadBranch(i_BranchToDelete)) {
                System.out.println("this branch is head brunch,can't delete it");
                return;
            }
            if(isLoadedToBranchObejcts(i_BranchToDelete)) {
                m_CurrentRepository.getM_Branches().remove(i_BranchToDelete);
            }
                deleteBranch(i_BranchToDelete);
        }
        else
        {
            throw new FileNotFoundException("Branch can't be found on this repository!");
        }
    }

    public List<String> showActiveBranchHistory() throws IOException {
        String activeBranchName = this.getHeadBranchFromRepository();
        Branch activeBranch = this.m_CurrentRepository.getM_Branches().get(activeBranchName);
        List<String> formatToPrint = new ArrayList<>();
        showCommitsHistoryRec(activeBranch.getM_PointedCommitId(), formatToPrint);
        return formatToPrint;
    }

    private void showCommitsHistoryRec(String i_CommitId, List<String> i_FormatToPrint) {

        String[] commitContent = commitDetailsFormatGetter(this.m_CurrentRepository.getM_Location(), i_CommitId);
        if (commitContent[1] == null || commitContent[1].isEmpty()) {
            i_FormatToPrint.add(i_CommitId);
            addToList(i_FormatToPrint,commitContent);
        }
        else {
            i_FormatToPrint.add(i_CommitId);
            addToList(i_FormatToPrint,commitContent);
            showCommitsHistoryRec(commitContent[1], i_FormatToPrint);
        }
    }

    private String[] commitDetailsFormatGetter(String i_RepositoryFullPath, String i_CommitId) {
        String commitContent = FileZipper.unZip(i_RepositoryFullPath + "/.magit/objects/" + i_CommitId);
        String[] splittedCommitContent = commitContent.split(";");
        return splittedCommitContent;
    }

    private void addToList(List<String> i_OutPutList, String[] i_ContentToAdd){
        i_OutPutList.add(i_ContentToAdd[2]);
        i_OutPutList.add(i_ContentToAdd[3]);
        i_OutPutList.add(i_ContentToAdd[4]);
    }

    public void showWorkingCopyStatus(List<String> i_ChangeFile,List<String> i_DeleteFile,List<String> i_CreateFile) throws IOException,NullPointerException {
        try {
            if( !(this.m_CurrentRepository == null)) {
                changesInCurrentWC(Paths.get(this.m_CurrentRepository.getM_Location() + "/WC"), i_ChangeFile, i_DeleteFile, i_CreateFile);
            }
            else
            {
                throw new NullPointerException("No repository is loaded!Can't show the status!");
            }
        } catch (IOException e) {
            throw new IOException("Can't find the repository path!Please try again.");
        }
    }

    public void changesInCurrentWC(Path i_WcFullPath,List<String> i_ChangeFile,List<String> i_DeleteFile,List<String> i_CreateFile) throws IOException {
        List<String> changeFile = new ArrayList<>();
        List<String> deleteFile = new ArrayList<>();
        List<String> createFile = new ArrayList<>();
        String commitSHA = null;
        String headFolderSHA = null;

        File folder = new File(i_WcFullPath.toString());

        for (Map.Entry<String, Branch> branch : this.getM_Repositories().getM_Branches().entrySet()) {
            Branch v = branch.getValue();
            if (v.isM_IsHead()) {
                commitSHA = v.getM_PointedCommitId();
                break;
            }
        }
        if (commitSHA != null && !(commitSHA.equals(""))) {
            headFolderSHA = this.getM_Repositories().getM_Commits().get(commitSHA).getM_RootFolderId();
            Folder headFolder = this.getM_Repositories().getM_Folders().get(headFolderSHA);
            Map<String, Folder.Item> itemHeadFolder = headFolder.getM_ItemsClone();
            checkItems(folder.listFiles(),itemHeadFolder,changeFile,deleteFile,createFile);
        }
        else{
            newFolderInWC(folder.listFiles(),i_CreateFile);
        }

        copyListToOther(changeFile,i_ChangeFile);
        copyListToOther(deleteFile,i_DeleteFile);
        copyListToOther(createFile,i_CreateFile);

    }

    private void checkItems(File[] i_FileList, Map<String, Folder.Item> i_Items, List<String> i_ChangeFile, List<String> i_DeleteFile, List<String> i_CreateFile) throws IOException {
        boolean isExist = false;
        for (File fileEntry : i_FileList) {
            for (Map.Entry<String, Folder.Item> entry : i_Items.entrySet()) {
                String k = entry.getKey();
                Folder.Item v = entry.getValue();
                if (v.getM_Name().equals(fileEntry.getName())) {
                    isExist = true;
                    if (v.getM_Type() == Folder.Item.eItemType.FOLDER) {
                        Folder checkFolder = this.getM_Repositories().getM_Folders().get(k);
                        checkItems(fileEntry.listFiles(),checkFolder.getM_ItemsClone(), i_ChangeFile,i_DeleteFile,i_CreateFile);
                    } else {
                        byte[] fileContent = Files.readAllBytes(Paths.get(fileEntry.getPath()));
                        Blob checkBlob = this.getM_Repositories().getM_Blobs().get(k);

                        if(!(new String (fileContent).trim().equals(checkBlob.getM_Content().trim()))){
                            i_ChangeFile.add(v.getM_Name());
                        }
                    }
                    i_Items.remove(k);
                    break;
                }
            }
            if (!isExist) {
                i_CreateFile.add(fileEntry.getName());
                if(!fileEntry.isFile()){
                    newFolderInWC(fileEntry.listFiles(),i_CreateFile);
                }

            }
            isExist = false;
        }


        if(i_Items.size()>0){
            i_Items.forEach((k, v) ->
            {
               i_DeleteFile.add(v.getM_Name());
            });
        }

    }

    public void newFolderInWC(File [] i_File, List<String> i_CreateFile){
        for(File fileEntry : i_File){
            i_CreateFile.add(fileEntry.getName());
            if(!fileEntry.isFile()){
                newFolderInWC(fileEntry.listFiles(),i_CreateFile);
            }
        }

    }

    public List<String> getAllBranchesToString(){
        List<String> branchesNamesList = new ArrayList<>();
        String pathBranches = (this.getM_Repositories().getM_Location() + "/.magit/branches");
        File folder = new File(pathBranches);

        for (File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (!(fileEntry.getName().toLowerCase().equals("head.txt"))) {
                branchesNamesList.add(fileEntry.getName());
                }
            }
        return branchesNamesList;
    }

    public void spanCheckoutBranchToOurObjects(String i_RepositoryFullPath,String i_SelectedBranch) throws FileNotFoundException, IOException {

        File file = new File(i_RepositoryFullPath + "/.magit/branches/" + i_SelectedBranch);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String pointedCommitId = br.readLine();
        br.close();

        if (m_CurrentRepository != null) {
            clearRepository();
        }
        m_CurrentRepository = new Repository(i_RepositoryFullPath);
        m_CurrentRepository.getM_Branches().put(i_SelectedBranch, new Branch(i_SelectedBranch, pointedCommitId, true));
        spanCommitToOurObjects(i_RepositoryFullPath, pointedCommitId);
    }

    public boolean checkIfThereAreOpenChanges(){
        boolean resultOfTest = false;
        try {
            resultOfTest = openChangesBeforeCheckout(Paths.get(this.m_CurrentRepository.getM_Location()+"/WC"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultOfTest;
    }

    public boolean openChangesBeforeCheckout(Path i_WcFullPath) throws IOException {

        boolean isExist = false;
        boolean isOpenChanges = false;
        String commitSHA = null;
        String headFolderSHA = null;

        BufferedReader br = null;
        List<String> changeFile = new ArrayList<>();
        List<String> deleteFile = new ArrayList<>();
        List<String> createFile = new ArrayList<>();

        File folder = new File(i_WcFullPath.toString());
        for (Map.Entry<String, Branch> branch : this.getM_Repositories().getM_Branches().entrySet()) {
            Branch v = branch.getValue();
            if (v.isM_IsHead()) {
                commitSHA = v.getM_PointedCommitId();
                break;
            }
        }

        if (commitSHA != null && !(commitSHA.equals(""))) {
            headFolderSHA = this.getM_Repositories().getM_Commits().get(commitSHA).getM_RootFolderId();
            Folder headFolder = this.getM_Repositories().getM_Folders().get(headFolderSHA);
            Map<String, Folder.Item> itemHeadFolder = headFolder.getM_ItemsClone();
            checkItems(folder.listFiles(),itemHeadFolder,changeFile,deleteFile,createFile);
        }


        if(changeFile.size()>0 || deleteFile.size()>0 || createFile.size()>0)
        {
            isOpenChanges = true;
        }
        return isOpenChanges;
    }

    private void copyListToOther(List<String> copyFrom , List<String> copyTo) {
        for(String str : copyFrom)
        {
            copyTo.add(str);
        }
    }

    public void createCommit() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:sss");
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the commit description:");
        String messege = scanner.nextLine();
        String author = this.m_ActiveUserName;
        String dateOfCreation = formatter.format(new Date());

        String prevCommitId = null;
        String headBrunch = null;
        String headFolder = null;

        for (Map.Entry<String, Branch> branch : this.getM_Repositories().getM_Branches().entrySet()) {
            Branch v = branch.getValue();
            if (v.isM_IsHead()) {
                prevCommitId = v.getM_PointedCommitId();
                headBrunch = v.getM_Name();
                break;
            }
        }

        try {
            headFolder = creatHeadFolderFromWC();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (headFolder == null && prevCommitId !=null) {
            headFolder = this.m_CurrentRepository.getM_Commits().get(prevCommitId).getM_RootFolderId();
        }
        if(headFolder == null && prevCommitId == null){
            File folder = new File(this.m_CurrentRepository.getM_Location()+"/WC");
            try {
                Folder head = new Folder(true);
                creatNewHeadFolder(folder.listFiles(),head);
                headFolder = DigestUtils.sha1Hex(head.toString());
                FileTime date = Files.getLastModifiedTime(folder.toPath());
                this.getM_Repositories().getM_Folders().put(headFolder, head);
                Folder.Item newItem = new Folder.Item(folder.getName(), headFolder, Folder.Item.eItemType.FOLDER, this.m_ActiveUserName, formatter.format(date.toMillis()));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        Commit newCommit = new Commit(headFolder,messege,author,dateOfCreation,prevCommitId);
        String newCommitId = DigestUtils.sha1Hex(newCommit.ToStringForSha1());
        this.getM_Repositories().getM_Commits().put(newCommitId,newCommit);
        this.getM_Repositories().getM_Branches().get(headBrunch).setM_PointedCommitId(newCommitId);
        try {
            Deleter.deleteDir(this.m_CurrentRepository.getM_Location());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.createRepository();
        Creator.createWC(this);
    }

    public String creatHeadFolderFromWC() throws IOException {
      String headFolderId = null;
      String commitSHA = null;
      String headFolderSHA = null;
        File folder = new File(this.m_CurrentRepository.getM_Location()+"/WC");
        for (Map.Entry<String, Branch> branch : this.getM_Repositories().getM_Branches().entrySet()) {
            Branch v = branch.getValue();
            if (v.isM_IsHead()) {
                commitSHA = v.getM_PointedCommitId();
                break;
            }
        }
        if (commitSHA != null) {
            headFolderSHA = this.getM_Repositories().getM_Commits().get(commitSHA).getM_RootFolderId();
            Folder headFolder = this.getM_Repositories().getM_Folders().get(headFolderSHA);
            Map<String, Folder.Item> itemHeadFolder = headFolder.getM_ItemsClone();
            Folder newHeadFolder = updateRepostory(folder.listFiles(), itemHeadFolder);
            if (newHeadFolder != null) {
                headFolderId = DigestUtils.sha1Hex(newHeadFolder.toString());
                this.getM_Repositories().getM_Folders().put(headFolderId, newHeadFolder);
            }
        }
      return headFolderId;
    }

    private Folder updateRepostory(File[] i_FileList, Map<String, Folder.Item> i_Items) throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:sss");
        boolean isExist = false;
        String sha1;
        Blob newBlob;
        FileTime date;
        Folder.Item newItem;
        Folder newFolder = null;
        byte[] fileContent;
        for (File fileEntry : i_FileList) {
            for (Map.Entry<String, Folder.Item> entry : i_Items.entrySet()) {
                String k = entry.getKey();
                Folder.Item v = entry.getValue();
                if (v.getM_Name().equals(fileEntry.getName())) {
                    isExist = true;
                    if (v.getM_Type() == Folder.Item.eItemType.FOLDER) {
                        Folder checkFolder = this.getM_Repositories().getM_Folders().get(k);
                        Folder newInsideFolder = updateRepostory(fileEntry.listFiles(),checkFolder.getM_Items());
                        if(newInsideFolder != null){
                            sha1 = DigestUtils.sha1Hex(newInsideFolder.toString());
                            date = Files.getLastModifiedTime(fileEntry.toPath());
                            this.getM_Repositories().getM_Folders().put(sha1, newInsideFolder);
                            newItem = new Folder.Item(fileEntry.getName(), sha1, Folder.Item.eItemType.FOLDER, this.m_ActiveUserName, formatter.format(date.toMillis()));
                        }
                        else{
                            newItem = v;
                        }
                        if (newFolder == null) {
                            newFolder = new Folder(false);
                        }
                        newFolder.getM_Items().put(newItem.getM_Id(), newItem);
                    } else {
                        fileContent = Files.readAllBytes(Paths.get(fileEntry.getPath()));
                        Blob checkBlob = this.getM_Repositories().getM_Blobs().get(k);

                        if (!(Arrays.equals(fileContent , checkBlob.getM_Content().getBytes()))){
                            sha1 = DigestUtils.sha1Hex(fileContent);
                            newBlob = new Blob(new String(fileContent));
                            date = Files.getLastModifiedTime(fileEntry.toPath());
                            this.getM_Repositories().getM_Blobs().put(sha1,newBlob);
                            newItem = new Folder.Item(fileEntry.getName(),sha1, Folder.Item.eItemType.BLOB,this.m_ActiveUserName,formatter.format(date.toMillis()));
                            if(newFolder == null){
                                newFolder = new Folder(false);
                            }
                            newFolder.getM_Items().put(newItem.getM_Id(),newItem);
                        }
                    }
                    break;
                }
            }
            if (!isExist) {
                if (fileEntry.isFile()) {
                    fileContent = Files.readAllBytes(Paths.get(fileEntry.getPath()));
                    sha1 = DigestUtils.sha1Hex(new String (fileContent));
                    newBlob = new Blob(new String(fileContent));
                    date = Files.getLastModifiedTime(fileEntry.toPath());
                    this.getM_Repositories().getM_Blobs().put(sha1, newBlob);
                    newItem = new Folder.Item(fileEntry.getName(), sha1, Folder.Item.eItemType.BLOB, this.m_ActiveUserName, formatter.format(date.toMillis()));
                }
                else{
                    Folder newInsideFolder = creatNewInsideFolder(fileEntry.listFiles());
                    sha1 = DigestUtils.sha1Hex(newInsideFolder.toString());
                    date = Files.getLastModifiedTime(fileEntry.toPath());
                    this.getM_Repositories().getM_Folders().put(sha1, newInsideFolder);
                    newItem = new Folder.Item(fileEntry.getName(), sha1, Folder.Item.eItemType.FOLDER, this.m_ActiveUserName, formatter.format(date.toMillis()));

                }
                if (newFolder == null) {
                    newFolder = new Folder(false);
                }
                newFolder.getM_Items().put(newItem.getM_Id(), newItem);
            }

            isExist = false;
        }

        if(newFolder != null)
        {
            boolean toAdd = true;
            for (File fileEntry : i_FileList) {
                toAdd = true;
                for (Map.Entry<String, Folder.Item> entry : newFolder.getM_Items().entrySet()) {
                    Folder.Item v = entry.getValue();
                    if ((fileEntry.getName().equals(v.getM_Name()))) {
                        toAdd = false;
                    }
                }

                if (toAdd) {

                    for (Map.Entry<String, Folder.Item> e : i_Items.entrySet()) {
                        String key = e.getKey();
                        Folder.Item valueToAdd = e.getValue();
                        Folder.Item value = e.getValue();
                        if (fileEntry.getName().equals(valueToAdd.getM_Name())) {
                            newFolder.getM_Items().put(key, value);
                        }

                    }


                }
            }
        }

        return newFolder;
    }

    public Folder creatNewInsideFolder(File[] i_Files) throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:sss");
        String sha1;
        Blob newBlob;
        FileTime date;
        Folder.Item newItem;
        byte[] fileContent;
        Folder newFolder= null;
        for (File fileEntry : i_Files) {
            if (fileEntry.isFile()) {
                fileContent = Files.readAllBytes(Paths.get(fileEntry.getPath()));
                sha1 = DigestUtils.sha1Hex(fileContent);
                newBlob = new Blob(new String(fileContent));
                date = Files.getLastModifiedTime(fileEntry.toPath());
                this.getM_Repositories().getM_Blobs().put(sha1, newBlob);
                newItem = new Folder.Item(fileEntry.getName(), sha1, Folder.Item.eItemType.BLOB, this.m_ActiveUserName, formatter.format(date.toMillis()));
                if (newFolder == null) {
                    newFolder = new Folder(false);
                }
                newFolder.getM_Items().put(newItem.getM_Id(), newItem);
            }
            else{
                Folder newInsideFolder = creatNewInsideFolder(fileEntry.listFiles());
            }
        }
        return newFolder;
    }

    public void creatNewHeadFolder(File[] i_Files, Folder i_Folder) throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:sss");
        String sha1;
        Blob newBlob;
        FileTime date;
        Folder.Item newItem;
        byte[] fileContent;
        for (File fileEntry : i_Files) {
            if (fileEntry.isFile()) {
                fileContent = Files.readAllBytes(Paths.get(fileEntry.getPath()));
                sha1 = DigestUtils.sha1Hex(fileContent);
                newBlob = new Blob(new String(fileContent));
                date = Files.getLastModifiedTime(fileEntry.toPath());
                this.getM_Repositories().getM_Blobs().put(sha1, newBlob);
                newItem = new Folder.Item(fileEntry.getName(), sha1, Folder.Item.eItemType.BLOB, this.m_ActiveUserName, formatter.format(date.toMillis()));
                i_Folder.getM_Items().put(newItem.getM_Id(), newItem);
            }
            else{
                Folder newInsideFolder = new Folder(false);
                creatNewHeadFolder(fileEntry.listFiles(),newInsideFolder);
                sha1 = DigestUtils.sha1Hex(newInsideFolder.toString());
                date = Files.getLastModifiedTime(fileEntry.toPath());
                this.getM_Repositories().getM_Folders().put(sha1, newInsideFolder);
                newItem = new Folder.Item(fileEntry.getName(), sha1, Folder.Item.eItemType.FOLDER, this.m_ActiveUserName, formatter.format(date.toMillis()));

            }
        }

    }

    public  void createRepository() {
        String pathObjects = ".magit/objects";
        String pathBranches = ".magit/branches";
        Path objectsPath = Paths.get(this.getM_Repositories().getM_Location() + "/" + pathObjects);
        Path branchesPath = Paths.get(this.getM_Repositories().getM_Location() + "/" + pathBranches);
        try {
            Files.createDirectories(objectsPath);
            Files.createDirectories(branchesPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.getM_Repositories().getM_Blobs().forEach((k, v) ->
        {
            FileZipper.Zip(Paths.get(objectsPath + "/" + k), Writer.writeBlob(v));
        });

        this.getM_Repositories().getM_Folders().forEach((k, v) ->
        {
            FileZipper.Zip(Paths.get(objectsPath + "/" + k), Writer.writeFolder(v));
        });

        this.getM_Repositories().getM_Commits().forEach((k, v) ->
        {
            FileZipper.Zip(Paths.get(objectsPath + "/" + k), Writer.writeCommit(v));
        });

        this.getM_Repositories().getM_Branches().forEach((k, v) ->
        {
            try {
                if(k.indexOf("repo2") == -1) {
                    if(v.isM_IsHead())
                    {
                        Files.write(Paths.get(branchesPath + "/" + "Head.txt"), k.getBytes());
                    }

                    Files.write(Paths.get(branchesPath + "/" + k), v.getM_PointedCommitId().getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    public void initializeNewRepository(String i_LocationOfrepository,String i_RepositoryName) throws Exception {

        String repositoryLocation = i_LocationOfrepository;
        Path repositoryPath = Paths.get(repositoryLocation + "/" + i_RepositoryName);
        if(Files.exists(repositoryPath))
        {
            throw new Exception("There is another file on that Location with identical name!");
        }
        String magitFolder = ".magit";
        String pathObjects = "objects";
        String pathBranches = "branches";
        String workingCopy = "WC";
        String headFile = "Head.txt";
        Branch masterBranch = new Branch("master",null,true);
        Path magitPath = Paths.get(repositoryPath + "/" + magitFolder);
        Path workingCopyPath = Paths.get(repositoryPath + "/" + workingCopy);
        Path objectsPath = Paths.get(magitPath + "/" + pathObjects);
        Path branchesPath = Paths.get(magitPath + "/" + pathBranches);
        Path headFilePath = Paths.get(branchesPath + "/" + headFile);
        File masterFile = new File(Paths.get(String.valueOf(repositoryPath),
                ".magit", "branches", "master").toString());
        try {
            Files.createDirectories(repositoryPath);
            Files.createDirectories(magitPath);
            Files.createDirectories(workingCopyPath);
            Files.createDirectories(objectsPath);
            Files.createDirectories(branchesPath);
            Files.createFile(headFilePath);
            Writer.WriteToTextFile(headFilePath,masterBranch.getM_Name());
            masterFile.createNewFile();
            }
        catch (IOException e) {
                e.printStackTrace();
            }

    }

    public void checkIfFileExistsInObjectsBySha1(String i_Sha1) throws Exception {
        if(m_CurrentRepository != null) {
            String objectsLocation = (this.m_CurrentRepository.getM_Location() + "/.magit/objects");
            Path obejctPath = Paths.get(objectsLocation + "/" + i_Sha1);
            if(!Files.exists(obejctPath))
            {
                throw new Exception("This Sha1 can't be found on objects folder!");
            }
        }
        else
        {
            throw new Exception("No repoistory is loaded!Therefore can't find this sha1 on objects!");
        }

    }

    public void resetActiveBranch(String i_NewCommitSha1) throws IOException {
        String activeBranchName = getHeadBranchFromRepository();
        writeNewSha1ToBranchFile(i_NewCommitSha1,activeBranchName);
        m_CurrentRepository.getM_Branches().get(activeBranchName).setM_PointedCommitId(i_NewCommitSha1);
        spanHeadBranchToOurObjects(m_CurrentRepository.getM_Location());
    }

    private  void writeNewSha1ToBranchFile(String i_NewCommitSha1,String i_ActiveBranchName) throws IOException {
        String branchesFile = (m_CurrentRepository.getM_Location() + "/.magit/branches/" + i_ActiveBranchName);
        Path activeBranchPath = Paths.get(branchesFile);
        Deleter.deleteTextFileContent(activeBranchPath);
        Writer.WriteToTextFile(activeBranchPath,i_NewCommitSha1);
    }

    public void changeTheActiveBranchActivity(String i_NewActiveBranchName) throws IOException {

        for(Map.Entry<String,Branch> entry: m_CurrentRepository.getM_Branches().entrySet())
        {
            entry.getValue().setM_IsHead(false);
        }
        File file = new File(m_CurrentRepository.getM_Location() + "/.magit/branches/" + i_NewActiveBranchName);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String pointedCommitId = br.readLine();
        br.close();
        try {
            m_CurrentRepository.getM_Branches().get(i_NewActiveBranchName).setM_IsHead(true);
        }
        catch (Exception ex) //if the branch isn't loaded to our objects
        {
            Branch addBranch = new Branch(i_NewActiveBranchName,pointedCommitId,true);
            m_CurrentRepository.getM_Branches().put(i_NewActiveBranchName,addBranch);
            String [] commitContent = commitDetailsFormatGetter(m_CurrentRepository.getM_Location(),pointedCommitId);
            m_CurrentRepository.getM_Commits().put(pointedCommitId,Creator.createCommitFromSplittedCOntent(commitContent));
        }
        Path headFile = Paths.get(m_CurrentRepository.getM_Location() + "/.magit/branches/Head.txt");
        Deleter.deleteTextFileContent(headFile);
        Writer.WriteToTextFile(headFile,i_NewActiveBranchName);
    }

    public void isRepositoryNull() throws NullPointerException {
        if(this.getM_Repositories() == null)
            throw new NullPointerException("No repository is loaded!please try again!");
    }

    public boolean isThereAnyBranch() {
        boolean result = true;
        int size = this.getM_Repositories().getM_Branches().size();
        if (size == 0) {
            System.out.println("No branches available!");
            result = false ;
        }
        return result;
    }

    public void SaveRepositoryToXml(String i_XmlFullPath) throws IOException {
        spreadAllBranchesInoOurObjects();
        spanAllNonPointedCommitsToOurObjects();
        m_SaveRepositoryToXML = new SaverToXml(m_CurrentRepository);
        m_SaveRepositoryToXML.SaveRepositoryToXml(i_XmlFullPath);
    }

    private void spreadAllBranchesInoOurObjects() throws IOException {
        List<String> branchesNames = Creator.getFileNamesOfFilesInAFolder(m_CurrentRepository.getM_Location() + "/.magit/branches");
        branchesNames.remove(branchesNames.indexOf("Head.txt"));

        for(String branchName: branchesNames) {
            if(!m_CurrentRepository.getM_Branches().containsKey(branchName)) {
                File file = new File(m_CurrentRepository.getM_Location() + "/.magit/branches/" + branchName);
                BufferedReader br = new BufferedReader(new FileReader(file));
                String pointedCommitId = br.readLine();
                br.close();

                Branch branch = new Branch(branchName, pointedCommitId, true);
                m_CurrentRepository.getM_Branches().put(branchName, branch);
                if (pointedCommitId != null && !pointedCommitId.equals("")) {
                    spanCommitToOurObjects(m_CurrentRepository.getM_Location(), pointedCommitId);
                }
            }
        }
    }

    public void deleteBranch(String i_BranchNameToDelete) {
        File branchToDelete = new File(String.valueOf(Paths.get(m_CurrentRepository.getM_Location(),
                ".magit", "branches", i_BranchNameToDelete)));
        FileUtils.deleteQuietly(branchToDelete);
    }

    private boolean isHeadBranch(String i_OtherBranchName) throws IOException {
        return this.getHeadBranchFromRepository().equals(i_OtherBranchName);
    }

    private boolean isLoadedToBranchObejcts(String i_ValueToSearch){
        return m_CurrentRepository.getM_Branches().containsKey(i_ValueToSearch);
    }

    private String extractRepositoryNameFromPath(String i_RepositoryFullPath){
        String name = i_RepositoryFullPath;
        int indexName = name.lastIndexOf("/");
        if(indexName == -1){
            indexName = name.lastIndexOf("\\");
        }
        return name.substring(indexName + 1);
    }
    public void spreadAllBranchesIntoOurObjects() throws IOException {
        List<String> branchesNames = Creator.getFileNamesOfFilesInAFolder(m_CurrentRepository.getM_Location() + "/.magit/branches");
        branchesNames.remove(branchesNames.indexOf("Head.txt"));

        for(String branchName: branchesNames) {
            if(!m_CurrentRepository.getM_Branches().containsKey(branchName)) {
                File file = new File(m_CurrentRepository.getM_Location() + "/.magit/branches/" + branchName);
                BufferedReader br = new BufferedReader(new FileReader(file));
                String pointedCommitId = br.readLine();
                br.close();

                Branch branch = new Branch(branchName, pointedCommitId);
                m_CurrentRepository.getM_Branches().put(branchName, branch);
                if (pointedCommitId != null && !pointedCommitId.equals("")) {
                    spanCommitToOurObjects(m_CurrentRepository.getM_Location(), pointedCommitId);
                }
            }
        }
    }

    public void spanAllNonPointedCommitsToOurObjects() throws IOException {
        String activeBranch = getHeadBranchFromRepository();
        String pointedCommitSha1 = m_CurrentRepository.getM_Branches().get(activeBranch).getM_PointedCommitId();
        if(pointedCommitSha1!=null) {
            addCommitsHistoryRec(pointedCommitSha1);
        }
    }

    private void addCommitsHistoryRec(String i_CommitId) throws IOException {
        String[] commitContent = commitDetailsFormatGetter(this.m_CurrentRepository.getM_Location(), i_CommitId);
        if (commitContent[1] == null || commitContent[1].isEmpty()) {
            spanCommitToOurObjects(this.m_CurrentRepository.getM_Location(),i_CommitId);
        }
        else {
            spanCommitToOurObjects(this.m_CurrentRepository.getM_Location(),i_CommitId);
            addCommitsHistoryRec(commitContent[1]);
        }
    }



}

