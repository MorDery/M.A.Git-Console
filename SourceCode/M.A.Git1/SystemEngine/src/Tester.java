import jaxb.schema.generated.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tester {
    private boolean m_IsValid;
    private List<String> ErrorMessage;

    public Tester(boolean m_IsValid) {
        this.m_IsValid = m_IsValid;
        ErrorMessage = new ArrayList<>();
    }

    public boolean isM_IsValid() {
        return m_IsValid;
    }

    public List<String> getErrorMessage() {
        return ErrorMessage;
    }

    public void runTester(String i_locationToCheck,MagitRepository i_Repository){
        m_IsValid = isFileExistsAndXML(i_locationToCheck)&&checkUniquenessOfAllIdsInRepository(i_Repository)&&checkExistenceOfAllBlobsInFolders(i_Repository.getMagitFolders().getMagitSingleFolder(),i_Repository.getMagitBlobs().getMagitBlob())
        &&checkExistenceOfAllFolderInFolders(i_Repository.getMagitFolders().getMagitSingleFolder()) && checkIfFolderIdIsInInnerFolders(i_Repository.getMagitFolders().getMagitSingleFolder()) &&checkExistenceOfReferencesFromCommitsToFolders(i_Repository.getMagitCommits().getMagitSingleCommit(),i_Repository.getMagitFolders().getMagitSingleFolder())
        &&  checkReferencesFromCommitsToFoldersValidity(i_Repository.getMagitCommits().getMagitSingleCommit(),i_Repository.getMagitFolders().getMagitSingleFolder())&& checkBranchesValidity(i_Repository.getMagitBranches().getMagitSingleBranch(),i_Repository.getMagitCommits().getMagitSingleCommit())&& isHeadValid(i_Repository.getMagitBranches());
        //to do detailed string error if need based on m_isValid result
    }
    // check 3.1
    private boolean isFileExistsAndXML(String i_XMLPath) {
        boolean isFileExistsAndXML = false;
        File file = new File(i_XMLPath);

        if (file.exists()) {
            if (i_XMLPath.endsWith("xml")) {
                isFileExistsAndXML = true;
            }
        }
        else
        {
            ErrorMessage.add("XML file can't be found or isn't Valid!");
        }

        return isFileExistsAndXML;
    }

    // check 3.2
    private  boolean checkUniquenessOfAllIdsInRepository(MagitRepository i_Repository) {
        boolean result = (checkUniquenessOfAllIdsInBlobs(i_Repository.getMagitBlobs().getMagitBlob()) && checkUniquenessOfAllIdsInFolders(i_Repository.getMagitFolders().getMagitSingleFolder())
                && checkUniquenessOfAllIdsInCommits(i_Repository.getMagitCommits().getMagitSingleCommit()));
        return  result;
    }

    private boolean checkUniquenessOfAllIdsInBlobs(List<MagitBlob> i_Blobs) {
        Set<String> setOfIds = new HashSet<>();
        boolean checkUniquenessOfAllIdsInBlobs = true;

        for (MagitBlob blob : i_Blobs) {
            if (!setOfIds.add(blob.getId())) {
                checkUniquenessOfAllIdsInBlobs = false;
                ErrorMessage.add("Some of the blobs id's are identical!Not valid XML format!");
                break;
            }
        }

        return checkUniquenessOfAllIdsInBlobs;
    }

    private boolean checkUniquenessOfAllIdsInFolders(List<MagitSingleFolder> i_Folders) {
        Set<String> setOfIds = new HashSet<>();
        boolean checkUniquenessOfAllIdsInFolders = true;

        for (MagitSingleFolder folder : i_Folders) {
            if (!setOfIds.add(folder.getId())) {
                checkUniquenessOfAllIdsInFolders = false;
                ErrorMessage.add("Some of the folder id's are identical!Not valid XML format!");
                break;
            }
        }

        return checkUniquenessOfAllIdsInFolders;
    }

    private boolean checkUniquenessOfAllIdsInCommits(List<MagitSingleCommit> i_Commits) {
        Set<String> setOfIds = new HashSet<>();
        boolean checkUniquenessOfAllIdsInCommits = true;

        for (MagitSingleCommit commit : i_Commits) {
            if (!setOfIds.add(commit.getId())) {
                checkUniquenessOfAllIdsInCommits = false;
                ErrorMessage.add("Some of the commit id's are identical!Not valid XML format!");
                break;
            }
        }

        return checkUniquenessOfAllIdsInCommits;
    }

    // check 3.3
    private  boolean checkExistenceOfAllBlobsInFolders(List<MagitSingleFolder> i_Folders, List<MagitBlob> i_Blobs) {
        Set<String> setOfBlobsIds = new HashSet<>();
        Set<String> setOfBlobsIdsInSingleFolders = new HashSet<>();
        boolean checkExistenceOfAllBlobsInFolders = false;

        for (MagitBlob blob : i_Blobs) {
            setOfBlobsIds.add(blob.getId());
        }

        for(MagitSingleFolder folder: i_Folders) {
            getAllBlobsIdsInSingleFoldersRec(i_Folders, folder, setOfBlobsIdsInSingleFolders);
        }

        if(setOfBlobsIds.size() == setOfBlobsIdsInSingleFolders.size())
        {
            for(String str : setOfBlobsIdsInSingleFolders ) {
            setOfBlobsIds.remove(str);
        }
            if(setOfBlobsIds.size() == 0)
            {
                checkExistenceOfAllBlobsInFolders = true;
            }
            else
            {
                ErrorMessage.add("One or more of the Folders contains an ID to a not found Blob!");
            }
        }
        else
        {
            ErrorMessage.add("One or more of the Folders contains an ID to a not found Blob!");
        }

        return checkExistenceOfAllBlobsInFolders;
    }

    private  void getAllBlobsIdsInSingleFoldersRec(List<MagitSingleFolder> i_Folders, MagitSingleFolder i_Folder, Set<String> i_SetOfBlobsIdsInSingleFolders){
        List<Item> items = i_Folder.getItems().getItem();
        MagitSingleFolder nextFolder = null;

        for (Item item : items) {
            if (item.getType().equals("blob")) {
                i_SetOfBlobsIdsInSingleFolders.add(item.getId());
            }
            else {
                for (MagitSingleFolder folder : i_Folders) {
                    if (folder.getId().equals(item.getId())) {
                        nextFolder = folder;
                        break;
                    }
                }
                    getAllBlobsIdsInSingleFoldersRec(i_Folders, nextFolder, i_SetOfBlobsIdsInSingleFolders);
            }
        }
    }
    // check 3.4
    private boolean checkExistenceOfAllFolderInFolders(List<MagitSingleFolder> i_Folders) {
        Set<String> setOfFolderIds = new HashSet<>();
        Set<String> setOfFolderIdsInSingleFolders = new HashSet<>();
        boolean checkExistenceOfAllFoldersInFolders = false;

        for (MagitSingleFolder folder : i_Folders) {
            setOfFolderIds.add(folder.getId());
        }

        for(MagitSingleFolder folder: i_Folders) {
            getAllFoldersIdsInSingleFoldersRec(i_Folders, folder, setOfFolderIdsInSingleFolders);
            if(folder.isIsRoot()){
                setOfFolderIdsInSingleFolders.add(folder.getId());
            }
        }

        if(setOfFolderIds.size() == setOfFolderIdsInSingleFolders.size())
        {
            for(String str : setOfFolderIdsInSingleFolders ) {
                setOfFolderIds.remove(str);
            }
            if(setOfFolderIds.size() == 0)
            {
                checkExistenceOfAllFoldersInFolders = true;
            }
            else
            {
                ErrorMessage.add("One or more of the Folders contains an ID to a not found Folder!");
            }
        }

        return checkExistenceOfAllFoldersInFolders;
    }

    private void getAllFoldersIdsInSingleFoldersRec(List<MagitSingleFolder> i_Folders, MagitSingleFolder i_Folder, Set<String> i_SetOfFolderIdsInSingleFolders){
        List<Item> items = i_Folder.getItems().getItem();
        MagitSingleFolder nextFolder = null;

        if(!checkIfAllTypesAreBlobs(items)) {
            for (Item item : items) {
                if (item.getType().equals("folder")) {
                    i_SetOfFolderIdsInSingleFolders.add(item.getId());
                    for (MagitSingleFolder folder : i_Folders) {
                        if (folder.getId().equals(item.getId())) {
                            nextFolder = folder;
                            break;
                        }
                    }
                    getAllFoldersIdsInSingleFoldersRec(i_Folders, nextFolder, i_SetOfFolderIdsInSingleFolders);
                }
            }
        }
    }

    private  boolean checkIfAllTypesAreBlobs(List<Item> i_Items) {
        boolean checkIfAllTypesAreBlobs = true;

        for(Item item : i_Items)
        {
            if(item.getType().equals("folder"))
            {
                checkIfAllTypesAreBlobs = false;
            }
        }

        return  checkIfAllTypesAreBlobs;
    }

    //check 3.5
    private boolean checkIfFolderIdIsInInnerFolders(List<MagitSingleFolder> i_Folders) {
        List<Item> items = null;
        boolean checkIfFolderIdIsInInnerFolders = true;

        for(MagitSingleFolder folder : i_Folders){
            items = folder.getItems().getItem();
            for(Item item : items){
                if(folder.getId().equals(item.getId()) && item.getType().equals("folder")){
                    checkIfFolderIdIsInInnerFolders = false;
                    ErrorMessage.add("One or more of the Folders contains an ID to itself!");
                    break;
                }
            }
        }

        return checkIfFolderIdIsInInnerFolders;
    }

    // check 3.6
    private boolean checkExistenceOfReferencesFromCommitsToFolders(List<MagitSingleCommit> i_Commits, List<MagitSingleFolder> i_Folders) {
        boolean checkExistenceOfReferencesFromCommitsToFolders = true;
        Set<String> setOfFoldersIds = new HashSet<>();

        for (MagitSingleFolder folder : i_Folders) {
            setOfFoldersIds.add(folder.getId());
        }

        for (MagitSingleCommit commit : i_Commits) {
            // true if this set did not already contain the specified id
            if (setOfFoldersIds.add(commit.getRootFolder().getId())) {
                checkExistenceOfReferencesFromCommitsToFolders = false;
                ErrorMessage.add("One or more of the Commits contains an ID to not found Folder!");
                break;
            }
        }

        return checkExistenceOfReferencesFromCommitsToFolders;
    }

    // check 3.7
    private boolean checkReferencesFromCommitsToFoldersValidity(List<MagitSingleCommit> i_Commits, List<MagitSingleFolder> i_Folders) {
        boolean checkReferencesFromCommitsToFoldersValidity = true;
        Set<String> setOfRootFoldersIds = new HashSet<>();

        for (MagitSingleFolder folder : i_Folders) {
            if (folder.isIsRoot()) {
                setOfRootFoldersIds.add(folder.getId());
            }
        }

        for (MagitSingleCommit commit : i_Commits) {
            // true if this set did not already contain the specified id
            if (setOfRootFoldersIds.add(commit.getRootFolder().getId())) {
                checkReferencesFromCommitsToFoldersValidity = false;
                ErrorMessage.add("One or more of the Commits contains an ID to a not root Folder!");
                break;
            }
        }

        return checkReferencesFromCommitsToFoldersValidity;
    }

    // check 3.8
    private boolean checkBranchesValidity(List<MagitSingleBranch> i_Branches, List<MagitSingleCommit> i_Commits) {
        boolean checkBranchesValidity = true;
        Set<String> setOfCommitsIds = new HashSet<>();

        for(MagitSingleCommit commit : i_Commits) {
            setOfCommitsIds.add(commit.getId());
        }

        for(MagitSingleBranch branch : i_Branches){
            // true if this set did not already contain the specified id
            if(setOfCommitsIds.add(branch.getPointedCommit().getId())){
                if(branch.getPointedCommit().getId().equals("") && i_Branches.size()==1)
                {
                    continue;
                }
                else {
                    checkBranchesValidity = false;
                    ErrorMessage.add("One or more of the Branches contains an ID to not found Commit!");
                    break;
                }
            }
        }

        return checkBranchesValidity;
    }

    // check 3.9
    private boolean isHeadValid(MagitBranches i_MagitBranches) {
        boolean isHeadValid = false;
        String head = i_MagitBranches.getHead();
        List<MagitSingleBranch> branches = i_MagitBranches.getMagitSingleBranch();

        for(MagitSingleBranch branch : branches){
            if(branch.getName().equals(head)){
                isHeadValid = true;
                break;
            }
        }

        if(!isHeadValid)
        {
            ErrorMessage.add("The Head file doesn't contain a valid Branch name!");
        }
        return isHeadValid;
    }
}
