import jaxb.schema.generated.*;
import org.apache.commons.codec.digest.DigestUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

public class SchemaBasedJAXB {

    private final static String JAXB_XML_MAGIT_PACKAGE_NAME = "jaxb.schema.generated";
    private String m_XMLPath;
    private Tester m_XmlTester;
    private Map<String,String> m_OldIdToNewId = new HashMap<>();


    public SchemaBasedJAXB(String i_XMLPath)
    {
        m_XMLPath = i_XMLPath;
    }

    public Tester getM_XmlTester() {
        return m_XmlTester;
    }

    public Repository deserializeFromXML() throws FileNotFoundException, JAXBException {

        InputStream inputStream = null;
        inputStream = new FileInputStream(m_XMLPath);

        MagitRepository magitRepository = null;
        magitRepository = deserializeFrom(inputStream);


        if(isXMLValid(m_XMLPath, magitRepository)){
            return createObjects(magitRepository);
        }
        else{
            return null;
        }
    }
    // deserialize from xml to magit rep
    private static MagitRepository deserializeFrom(InputStream in) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(JAXB_XML_MAGIT_PACKAGE_NAME);
        Unmarshaller u = jc.createUnmarshaller();
        return (MagitRepository) u.unmarshal(in);
    }

    //create a tester to check the XML path
    private boolean isXMLValid(String i_XMLPath, MagitRepository i_Repository) {
        m_XmlTester = new Tester(false);
        m_XmlTester.runTester(i_XMLPath,i_Repository);
        return m_XmlTester.isM_IsValid();
    }
    //create the objects based on the magit repository
    private Repository createObjects(MagitRepository i_MagitRepository) {
        Repository myRepository = new Repository(i_MagitRepository.getLocation(),i_MagitRepository.getName());
        createBranches(myRepository.getM_Branches() ,i_MagitRepository.getMagitBranches());
        createCommits(myRepository.getM_Commits(),i_MagitRepository.getMagitCommits());
        createFoldersAndBlobs(myRepository.getM_Folders(),myRepository.getM_Blobs(),i_MagitRepository.getMagitFolders(),i_MagitRepository.getMagitBlobs());
        addSha1ToOurObjects(myRepository);
        return  myRepository;
    }
    //create the branches based on the magit repository
    private void createBranches(Map<String,Branch> i_Branches ,MagitBranches i_magitBranch){
        List<MagitSingleBranch> branches = i_magitBranch.getMagitSingleBranch();

        for(MagitSingleBranch branch : branches){
            i_Branches.put(branch.getName(),new Branch(branch.getName(), branch.getPointedCommit().getId(), branch.getTrackingAfter(), branch.isIsRemote(), branch.isTracking()));
        }
        i_Branches.get(i_magitBranch.getHead()).setM_IsHead(true);
    }

    //create the commits based on the magit repository
    private void createCommits(Map<String,Commit> i_Commits ,MagitCommits i_magitCommits){
        List<MagitSingleCommit> commits = i_magitCommits.getMagitSingleCommit();
        for(MagitSingleCommit commit : commits){
            Commit commitToAdd = new Commit(commit.getRootFolder().getId(),commit.getMessage(),commit.getAuthor(),commit.getDateOfCreation());
            if((commit.getPrecedingCommits() != null)) {
                commitToAdd.setM_PrecedingCommitId(initializeFormerCommits(commit.getPrecedingCommits().getPrecedingCommit()));
            }
            i_Commits.put(commit.getId(),commitToAdd);
        }
    }

    //create the preceding commits based on the magit repository
    private List <String> initializeFormerCommits (List<PrecedingCommits.PrecedingCommit> i_CommitsFromXML){
        List <String> Commits = new ArrayList<>();
        for(PrecedingCommits.PrecedingCommit commit : i_CommitsFromXML){
            Commits.add(commit.getId());
        }
        return  Commits;
    }

    //create the folders based on the magit repository
    private void createFoldersAndBlobs(Map<String,Folder> i_Folders,Map<String,Blob> i_Blobs,MagitFolders i_MagitFolders,MagitBlobs i_MagitBlobs){

        List<MagitBlob> magitBlobs = i_MagitBlobs.getMagitBlob();
        List<MagitSingleFolder> magitFolders = i_MagitFolders.getMagitSingleFolder();
        boolean isCurrentRoot = false;
        final String blobName = "blob";
        final String folderName = "folder";

        addBlobs(i_Blobs,magitBlobs);

        for(MagitSingleFolder magitFolder  : magitFolders){

            isCurrentRoot = magitFolder.isIsRoot();
            Folder folderToCreate = new Folder(isCurrentRoot);
            AttachFolderToOurMap(folderToCreate,i_Folders,magitFolder.getId());
            List<Item> magitItems = magitFolder.getItems().getItem();
            for (Item magitItem : magitItems){
                if(magitItem.getType().equals(blobName)){
                    folderToCreate.getM_Items().put(magitItem.getId(),createItemObjectFromMagitBlob(i_Blobs,magitBlobs,magitItem.getId()));
                }
                else{
                    folderToCreate.getM_Items().put(magitItem.getId(),createItemObjectFromMagitFolder(magitFolders,magitItem.getId()));
                }
            }
        }
    }

    //create blobs to add to the items map of each folder based on the magit repository
    private Folder.Item createItemObjectFromMagitBlob(Map<String,Blob> i_Blobs,List<MagitBlob> i_MagitBlobs,String i_BlobId){

        Folder.Item itemToAdd = null;
        for(MagitBlob blobToFind : i_MagitBlobs){
           if(blobToFind.getId().equals(i_BlobId)){
               itemToAdd = new Folder.Item(blobToFind.getName(),blobToFind.getId(), Folder.Item.eItemType.BLOB,blobToFind.getLastUpdater(),blobToFind.getLastUpdateDate());
           }
        }
        return itemToAdd;
    }

    //create folders to add to the items map of each folder based on the magit repository
    private Folder.Item createItemObjectFromMagitFolder(List<MagitSingleFolder> i_MagitFolders,String i_FolderId){

        Folder.Item itemToAdd = null;
        for(MagitSingleFolder folderToFind : i_MagitFolders){
            if(folderToFind.getId().equals(i_FolderId)){
                itemToAdd = new Folder.Item(folderToFind.getName(),folderToFind.getId(), Folder.Item.eItemType.FOLDER,folderToFind.getLastUpdater(),folderToFind.getLastUpdateDate());
            }
        }
        return itemToAdd;
    }

    //create blobs to add to our blobs Map based on the magit repository
    private void addBlobs(Map<String,Blob> i_Blobs,List<MagitBlob> i_MagitBlobs){
        for(MagitBlob blobToAdd : i_MagitBlobs) {
            i_Blobs.put(blobToAdd.getId(), new Blob(blobToAdd.getContent()));
        }
    }

    private void AttachFolderToOurMap(Folder i_CurrentFolder,Map<String,Folder> i_Folders,String i_Id){
        i_Folders.put(i_Id,i_CurrentFolder);
    }

    private void addSha1ToOurObjects(Repository repository) {
        List<Folder> rootFoldersList = new ArrayList<>();
        addSha1ToBranchesAndCommits(repository.getM_Branches(),repository.getM_Commits());

        for(Map.Entry<String,Folder> currentFolder :repository.getM_Folders().entrySet()) {
            if(currentFolder.getValue().isM_IsRoot()) {
                rootFoldersList.add(currentFolder.getValue());
            }
        }
        for(Folder rootFolder : rootFoldersList) {
            addSha1ToFoldersAndBlobs(rootFolder, repository.getM_Blobs(), repository.getM_Folders());
            String Sha1 = DigestUtils.sha1Hex(rootFolder.toString());
            repository.getM_Folders().put(Sha1,rootFolder);
            updateTheCommitRootFolderId(repository,rootFolder,Sha1);
        }
        cleanDuplicates(repository);
        addSha1OfPrecendingCommits(repository.getM_Commits());
    }

    private void addSha1ToBranchesAndCommits(Map<String,Branch> i_Branches , Map<String,Commit> i_Commits){

        Commit currentCommit = null ;

        if(i_Branches.size() == 1)
        {
            try{
                if((i_Branches.get("master").getM_PointedCommitId().equals("")) || (i_Branches.get("master").getM_PointedCommitId()==null)){
                    //empty repository from XML file - no need to create sha1
                    return;
                }
            }
            catch (Exception ex){
                //if it's not called master so we don't care
            }
        }
        for(Map.Entry<String,Branch> branch : i_Branches.entrySet())
        {
            String oldCommitId = branch.getValue().getM_PointedCommitId();
            currentCommit = i_Commits.get(oldCommitId);
            String sha1 = DigestUtils.sha1Hex(currentCommit.ToStringForSha1());
            m_OldIdToNewId.put(oldCommitId,sha1);
            i_Commits.put(sha1,currentCommit);
            branch.getValue().setM_PointedCommitId(sha1);
        }

        Map<String,Commit> fakeMap = copyCommitMap(i_Commits);

        for(Map.Entry<String,Commit> commit : fakeMap.entrySet())
        {
            if(commit.getKey().length() <39)//that commit isn't point by any branch so we didn't create sha1 for it
            {
                String oldCommitId = commit.getKey();
                currentCommit = i_Commits.get(oldCommitId);
                String sha1 = DigestUtils.sha1Hex(currentCommit.ToStringForSha1());
                m_OldIdToNewId.put(oldCommitId, sha1);
                i_Commits.put(sha1,currentCommit);
            }
        }



    }

    private void addSha1OfPrecendingCommits(Map<String,Commit> i_Commits) {
        for(Map.Entry<String,Commit> commit : i_Commits.entrySet())
        {
            try
            {
                String precendingCommitOldId = commit.getValue().getM_PrecedingCommitId().get(0);
                String precendingCommitNewId = m_OldIdToNewId.get(precendingCommitOldId);
                commit.getValue().setM_PrecedingCommitId(precendingCommitNewId);
            }
            catch (Exception ex)
            {
                //it will get here once the commit doesn't have any preceding commit.
                //so we don't really care about it .
            }
        }
    }

    private void addSha1ToFoldersAndBlobs(Folder i_currentFolder ,Map<String,Blob> i_Blobs ,Map<String,Folder> i_Folders ) {
        String Sha1 = null ;
        Folder currentFolder = null;
        Map <String,Folder.Item> cloneMap = copyOurMap(i_currentFolder.getM_Items());

        for(Map.Entry<String,Folder.Item> itemData : cloneMap.entrySet()){
            currentFolder = i_Folders.get(itemData.getValue().getM_Id());
            if(itemData.getValue().getM_Type() == Folder.Item.eItemType.BLOB){
                Blob temp = i_Blobs.get(itemData.getValue().getM_Id());
                Sha1 = DigestUtils.sha1Hex(temp.getM_Content());
                i_Blobs.put(Sha1,temp); // add to big blobs map
                i_currentFolder.getM_Items().remove(itemData.getValue().getM_Id()); // remove from the folder map
                itemData.getValue().setM_Id(Sha1);  // set new id based on Sha1
                i_currentFolder.getM_Items().put(Sha1,itemData.getValue()); //add to folder map
            }
            else if(allItemsAreBlobs(currentFolder))
            {
                    makeSha1ForMultiBlobs(currentFolder,i_Blobs);
                    Folder temp = i_Folders.get(itemData.getValue().getM_Id()); // find the folder
                    Sha1 = DigestUtils.sha1Hex(temp.toString()); //calculate sha1
                    i_Folders.put(Sha1,temp); //add to big map folders
                    i_currentFolder.getM_Items().remove(itemData.getValue().getM_Id());//remove from the folder map
                    itemData.getValue().setM_Id(Sha1); // set new id based on Sha1
                    i_currentFolder.getM_Items().put(Sha1,itemData.getValue()); //add to folder map
                }
            else
                {
                    Folder nextFolder = i_Folders.get(itemData.getValue().getM_Id());
                    addSha1ToFoldersAndBlobs(nextFolder,i_Blobs,i_Folders);// recursion call
                    Sha1 = DigestUtils.sha1Hex(nextFolder.toString()); //calculate the sha1
                    i_Folders.put(Sha1,nextFolder); //add to big map folders
                    i_currentFolder.getM_Items().remove(itemData.getValue().getM_Id()); //remove from the folder map
                    itemData.getValue().setM_Id(Sha1); // set new id based on Sha1
                    i_currentFolder.getM_Items().put(Sha1,itemData.getValue()); //add to folder map
                }
            }

        }

    private boolean allItemsAreBlobs(Folder i_Folder){

        boolean result = true;

        for(Map.Entry<String,Folder.Item> item : i_Folder.getM_Items().entrySet()){
            if(item.getValue().getM_Type() == Folder.Item.eItemType.FOLDER ){
                result = false;
                break;
            }
        }
        return result ;
    }

    private void makeSha1ForMultiBlobs(Folder i_Folder,Map<String,Blob> i_Blobs){
        String Sha1 = null;

        for(Map.Entry<String,Folder.Item> item : i_Folder.getM_Items().entrySet()){
            Blob temp = i_Blobs.get(item.getValue().getM_Id());
            Sha1 = DigestUtils.sha1Hex(temp.getM_Content());
            i_Blobs.put(Sha1,temp); // add to big blobs map
            i_Folder.getM_Items().remove(item.getValue().getM_Id()); // remove from the folder map
            item.getValue().setM_Id(Sha1); // set new id based on Sha1
            i_Folder.getM_Items().put(item.getValue().getM_Id(),item.getValue()); //add to folder map
        }
    }

    private Map <String,Folder.Item> copyOurMap(Map <String,Folder.Item> cloneMap) {
        Map<String, Folder.Item> result = new LinkedHashMap<>();
        for (Map.Entry<String, Folder.Item> item : cloneMap.entrySet()) {
            result.put(item.getKey(), item.getValue());
        }
        return result;
    }

    private Map <String,Commit> copyCommitMap(Map <String,Commit> cloneMap) {
        Map<String, Commit> result = new LinkedHashMap<>();
        for (Map.Entry<String,Commit> item : cloneMap.entrySet()) {
            result.put(item.getKey(), item.getValue());
        }
        return result;
    }

    private void updateTheCommitRootFolderId (Repository repository,Folder i_RootFolder,String i_NewId){

        Folder pointedFolder = null ;
        for(Map.Entry<String,Commit> currentCommit : repository.getM_Commits().entrySet()){
            pointedFolder = repository.getM_Folders().get(currentCommit.getValue().getM_RootFolderId());
            if(pointedFolder == i_RootFolder){
                currentCommit.getValue().setM_RootFolderId(i_NewId);
            }
        }

    }

    private void cleanDuplicates (Repository repository){

        List<String> toRemoveDuplicateFolders = new ArrayList<>();
        List<String> toRemoveDuplicateBlobs = new ArrayList<>();
        List<String> toRemoveDuplicateCommit = new ArrayList<>();
        removal(repository.getM_Folders(),toRemoveDuplicateFolders);
        removal(repository.getM_Blobs(),toRemoveDuplicateBlobs);
        removal(repository.getM_Commits(),toRemoveDuplicateCommit);
    }

    private <T> void removal(Map<String,T> map , List<String> dataBase){

        for(Map.Entry<String,T> current :map.entrySet()) {
            if(current.getKey().length() <=39) {
                dataBase.add(current.getKey());
            }
        }
        for(String str : dataBase){
            map.remove(str);
        }
    }
}
