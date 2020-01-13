import jaxb.schema.generated.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SaverToXml {
    private Repository m_Repository;
    private final static String JAXB_XML_MAGIT_PACKAGE_NAME = "jaxb.schema.generated";
    private final static int SHA1_LENGTH_LETTERS = 40;

    public SaverToXml(Repository i_Repository) {
        m_Repository = i_Repository;
    }

    private void magitRepositoryToXml(MagitRepository i_MagitRepo, String i_XmlPath) {
        try {
            File file = new File(i_XmlPath);
            JAXBContext jaxbContext = JAXBContext.newInstance(JAXB_XML_MAGIT_PACKAGE_NAME);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(i_MagitRepo, file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public void SaveRepositoryToXml(String i_XmlPath) {
        MagitRepository magitRepository = new MagitRepository();
        magitRepository.setLocation(m_Repository.getM_Location());
        magitRepository.setName(m_Repository.getM_Name());

        if (m_Repository.getM_Branches().size() > 0) {
            createMagitBranchesToXML(magitRepository);
        }

        if (m_Repository.getM_Commits().size() > 0) {
            createMagitCommitsToXML(magitRepository);
        }

        magitRepositoryToXml(magitRepository, i_XmlPath);
    }

    private void createMagitBranchesToXML(MagitRepository i_MagitRepository) {
        Map<String, Branch> branches = m_Repository.getM_Branches();
        i_MagitRepository.setMagitBranches(new MagitBranches());
        List<MagitSingleBranch> magitBranches = i_MagitRepository.getMagitBranches().getMagitSingleBranch();

        for (Map.Entry<String, Branch> branchEntry : branches.entrySet()) {
            MagitSingleBranch magitBranch = new MagitSingleBranch();
            MagitSingleBranch.PointedCommit pointedCommit = new MagitSingleBranch.PointedCommit();

            pointedCommit.setId(branchEntry.getValue().getM_PointedCommitId());
            magitBranch.setPointedCommit(pointedCommit);
            magitBranch.setTrackingAfter(branchEntry.getValue().getM_TrackingAfter());
            magitBranch.setTracking(branchEntry.getValue().getM_Tracking());
            magitBranch.setName(branchEntry.getValue().getM_Name());
            magitBranch.setIsRemote(branchEntry.getValue().getM_IsRemote());

            if (branchEntry.getValue().isM_IsHead()) {
                i_MagitRepository.getMagitBranches().setHead(magitBranch.getName());
            }

            magitBranches.add(magitBranch);
        }
    }

    private void createMagitCommitsToXML(MagitRepository i_MagitRepository) {
        Map<String, Commit> commits = m_Repository.getM_Commits();

        i_MagitRepository.setMagitCommits(new MagitCommits());
        i_MagitRepository.setMagitFolders(new MagitFolders());
        i_MagitRepository.setMagitBlobs(new MagitBlobs());
        Set<String> sha1TrackerSet = new HashSet<>();

        List<MagitSingleCommit> magitCommits = i_MagitRepository.getMagitCommits().getMagitSingleCommit();

        for (Map.Entry<String, Commit> commitEntry : commits.entrySet()) {
            if (commitEntry.getKey().length() == SHA1_LENGTH_LETTERS) {
                MagitSingleCommit magitCommit = new MagitSingleCommit();
                PrecedingCommits precedingCommits = new PrecedingCommits();
                List<PrecedingCommits.PrecedingCommit> magitPrecedingCommits = precedingCommits.getPrecedingCommit();

                PrecedingCommits.PrecedingCommit magitPrecedingCommit = new PrecedingCommits.PrecedingCommit();
                try {
                    magitPrecedingCommit.setId(commitEntry.getValue().getM_PrecedingCommitId().get(0));
                }
                catch (Exception ex){
                    magitPrecedingCommit.setId(null);
                }
                magitPrecedingCommits.add(magitPrecedingCommit);

                RootFolder magitRootFolder = new RootFolder();
                magitRootFolder.setId(commitEntry.getValue().getM_RootFolderId());
                magitCommit.setRootFolder(magitRootFolder);

                magitCommit.setPrecedingCommits(precedingCommits);
                magitCommit.setMessage(commitEntry.getValue().getM_Message());
                magitCommit.setId(commitEntry.getKey());
                magitCommit.setDateOfCreation(commitEntry.getValue().getM_DateOfCreation());
                magitCommit.setAuthor(commitEntry.getValue().getM_Author());

                magitCommits.add(magitCommit);

                // createMagitFolders
                MagitSingleFolder magitFolder = new MagitSingleFolder();
                magitFolder.setName(null);
                magitFolder.setLastUpdater(commitEntry.getValue().getM_Author());
                magitFolder.setLastUpdateDate(commitEntry.getValue().getM_DateOfCreation());
                magitFolder.setIsRoot(true);
                magitFolder.setId(commitEntry.getValue().getM_RootFolderId());

                createMagitFoldersToXML(i_MagitRepository, magitFolder, sha1TrackerSet);
            }
        }
    }

    private void createMagitFoldersToXML(MagitRepository i_MagitRepository, MagitSingleFolder i_CurrentMagitFolder, Set<String> i_Sha1TrackerSet) {
        List<MagitSingleFolder> magitFolders = i_MagitRepository.getMagitFolders().getMagitSingleFolder();
        magitFolders.add(i_CurrentMagitFolder);
        i_Sha1TrackerSet.add(i_CurrentMagitFolder.getId());

        MagitSingleFolder.Items items = new MagitSingleFolder.Items();
        List<Item> itemsList = items.getItem();
        i_CurrentMagitFolder.setItems(items);

        Folder folder = m_Repository.getM_Folders().get(i_CurrentMagitFolder.getId());

        for (Map.Entry<String, Folder.Item> entry : folder.getM_Items().entrySet()) {
            Folder.Item itemData = entry.getValue();
            Item item = new Item();
            item.setType(itemData.getM_Type().toString().toLowerCase());
            item.setId(itemData.getM_Id());
            itemsList.add(item);

            if (!i_Sha1TrackerSet.contains(itemData.getM_Id())) {
                if (itemData.getM_Type().equals(Folder.Item.eItemType.FOLDER)) {
                    MagitSingleFolder magitSubFolder = new MagitSingleFolder();
                    magitSubFolder.setId(itemData.getM_Id());
                    magitSubFolder.setIsRoot(false);
                    magitSubFolder.setLastUpdateDate(itemData.getLastUpdateDate());
                    magitSubFolder.setLastUpdater(itemData.getLastUpdater());
                    magitSubFolder.setName(itemData.getM_Name());

                    createMagitFoldersToXML(i_MagitRepository, magitSubFolder, i_Sha1TrackerSet);
                } else {
                    MagitBlob magitBlob = new MagitBlob();
                    magitBlob.setName(itemData.getM_Name());
                    magitBlob.setLastUpdater(itemData.getLastUpdater());
                    magitBlob.setLastUpdateDate(itemData.getLastUpdateDate());
                    magitBlob.setId(itemData.getM_Id());

                    createMagitBlobToXML(i_MagitRepository, magitBlob, i_Sha1TrackerSet);
                }
            }
        }
    }

    private void createMagitBlobToXML(MagitRepository i_MagitRepository, MagitBlob i_MagitBlob, Set<String> i_Sha1TrackerSet) {
        List<MagitBlob> magitBlobs = i_MagitRepository.getMagitBlobs().getMagitBlob();
        String blobContent = m_Repository.getM_Blobs().get(i_MagitBlob.getId()).getM_Content();

        i_MagitBlob.setContent(blobContent);
        magitBlobs.add(i_MagitBlob);
        i_Sha1TrackerSet.add(i_MagitBlob.getId());
    }

}

