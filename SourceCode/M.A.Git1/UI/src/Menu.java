
import org.apache.commons.io.FileUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Menu {

    private MAGit m_MyMagit;

    public Menu()
    {
        m_MyMagit = new MAGit("Administrator");
    }

    public enum eMenuOption {
        CHANGE_USER_NAME, LOAD_REPOSITORY_FROM_XML, SWITCH_REPOSITORY, SHOW_CURRENT_COMMIT_FILE_SYSTEM_INFORMATION,
        SHOW_WORKING_COPY_STATUS, COMMIT, LIST_AVAILABLE_BRANCHES, CREATE_NEW_BRANCH, DELETE_BRANCH,
        CHECKOUT_BRANCH, SHOW_CURRENT_BRANCH_HISTORY, INITIALIZE_NEW_REPOSITORY, RESET_ACTIVE_BRANCH, SAVE_TO_XML ,EXIT
    }

    public void run() {
        boolean isExit = false;
        int userChoise;

        do {
            printMenuTitle();
            printMenu();
            userChoise = getIntInputFromUser();
            isExit = activeUserSelection(eMenuOption.values()[userChoise]);
        } while (!isExit);
    }

    private void printMenuTitle() {
        String currentRepositoryLocation;

        if (m_MyMagit.getM_Repositories() == null) {
            currentRepositoryLocation = "N/A";
        } else {
            currentRepositoryLocation = m_MyMagit.getM_Repositories().getM_Location();
        }

        System.out.println("Magit Menu      Current User: " + m_MyMagit.getM_ActiveUserName() + "     Current repository location: " + currentRepositoryLocation);
    }

    private void printMenu() {
        System.out.println("0. Change user name" + System.lineSeparator()
                + "1. Load repository from XML" + System.lineSeparator()
                + "2. Switch repository" + System.lineSeparator()
                + "3. Show current commit file system information" + System.lineSeparator()
                + "4. Show working copy status" + System.lineSeparator()
                + "5. Commit" + System.lineSeparator()
                + "6. List available branches" + System.lineSeparator()
                + "7. Create new branch" + System.lineSeparator()
                + "8. Delete branch" + System.lineSeparator()
                + "9. Checkout branch" + System.lineSeparator()
                + "10. Show current branch history" + System.lineSeparator()
                + "11. Initialize new repository" + System.lineSeparator()
                + "12. Reset the active branch to a new commit" + System.lineSeparator()
                + "13. Save your repository to XML file" + System.lineSeparator()
                + "14. Exit");
    }

    private boolean activeUserSelection(eMenuOption i_UserSelection) {
        boolean isExit = false;

        switch (i_UserSelection) {
            case CHANGE_USER_NAME:
                changeUserName();
                break;
            case LOAD_REPOSITORY_FROM_XML:
                loadFromXMLProcess();
                break;
            case SWITCH_REPOSITORY:
                ChangeRepository();
                break;
            case SHOW_CURRENT_COMMIT_FILE_SYSTEM_INFORMATION:
                ShowCurrentCommitFiles();
                break;
            case SHOW_WORKING_COPY_STATUS:
                ShowWorkingCopyStatus();
                break;
            case COMMIT:
                commitFunction();
                break;
            case LIST_AVAILABLE_BRANCHES:
                listAvailableBranches();
                break;
            case CREATE_NEW_BRANCH:
                creatNewBranch();
                break;
            case DELETE_BRANCH:
                deleteBrunch();
                break;
            case CHECKOUT_BRANCH:
                checkOutBranch();
                break;
            case SHOW_CURRENT_BRANCH_HISTORY:
                showCurrentBranchHistory();
                break;
            case INITIALIZE_NEW_REPOSITORY:
                initializeNewRepository();
                break;
            case RESET_ACTIVE_BRANCH:
                resetActiveBranch();
                break;
            case SAVE_TO_XML:
                saveRepositoryToXML();
                break;
            case EXIT:
                isExit = true;
                break;
        }
        return isExit;
    }

    private void changeUserName() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter your name:");
        String userName = scanner.next();
        m_MyMagit.setM_ActiveUserName(userName);
    }

    private void loadFromXMLProcess() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter XML Path");
        String XMLPath = scanner.next();
        SchemaBasedJAXB converterFromXML = new SchemaBasedJAXB(XMLPath);
        try{
            m_MyMagit.setM_Repositories(converterFromXML.deserializeFromXML());
        } catch (FileNotFoundException e) {
            System.out.println("File wasn't found!Please try again!");
            return;
        } catch (JAXBException e) {
            System.out.println("Can't use the Schema XSD to deserialize!Please try again!");
            return;
        }
        boolean testerAnswer = checkIfAnswerIsValid();
        if(!testerAnswer)
        {
            //the xml isn't valid - let's print the errors
            printTheXMLErros(converterFromXML.getM_XmlTester());
            return;
        }
        boolean isExists = m_MyMagit.isRepositoryExists(m_MyMagit.getM_Repositories().getM_Location());

        if (!isExists) {
            //we need to create a repository based on the XML
            m_MyMagit.createRepository();
            Creator.createWC(m_MyMagit);
        } else {
            boolean isValidChoice = false;
            int userChoice;
            while (!isValidChoice) {
                System.out.println("Repository already exists! What would you like to perform ?");
                System.out.println("0. Delete Repository and create a new one" + System.lineSeparator()
                        + "1. Remain with the current Repository");
                userChoice = getIntInputFromUser();
                if (userChoice == 0) {
                    //we need to delete the current repository and create a new one
                    try {
                        Deleter.deleteDir(m_MyMagit.getM_Repositories().getM_Location());
                    }
                    catch (Exception ex)
                    {
                        System.out.println(ex.getMessage());
                        return;
                    }
                    m_MyMagit.createRepository();
                    Creator.createWC(m_MyMagit);
                    isValidChoice = true;
                } else if (userChoice == 1) {
                    //we need to remain the current repository and create our objects based on that
                    loadRepositoryToOurObjects(m_MyMagit.getM_Repositories().getM_Location());
                    isValidChoice = true;
                } else {
                    System.out.println("Not a valid choice,please try again!");
                }
            }
        }
    }

    private boolean checkIfAnswerIsValid() {
        if(m_MyMagit.getM_Repositories() == null)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    private void printTheXMLErros(Tester resultFromTester) {
        for(String str : resultFromTester.getErrorMessage()){
            System.out.println(str);
        }
    }

    private void ChangeRepository() {
        String repositoryFullPath = getInputStringFromTheUser("Please enter the repository full path:",
                "Invalid input! empty path was entered , try again:");
        loadRepositoryToOurObjects(repositoryFullPath);
    }

    private void ShowWorkingCopyStatus() {
        List<String> changeFile = new ArrayList<>();
        List<String> deleteFile = new ArrayList<>();
        List<String> createFile = new ArrayList<>();
        try{
            m_MyMagit.showWorkingCopyStatus(changeFile,deleteFile,createFile);
        } catch (NullPointerException | IOException e) {
            System.out.println(e.getMessage());
            return;
        }

        //print data
        System.out.println("Repository name:"+this.m_MyMagit.getM_Repositories().getM_Name());
        System.out.println("Repository location:" +this.m_MyMagit.getM_Repositories().getM_Location());
        System.out.println("User name:"+this.m_MyMagit.getM_ActiveUserName());

        System.out.println("Files that were changed:");
        if(changeFile !=null) {
            changeFile.forEach((k) ->
            {
                System.out.println(k);
            });
        }

        if(createFile !=null) {
            System.out.println("Files that are new:");
            createFile.forEach((k) ->
            {
                System.out.println(k);
            });
        }

        if(deleteFile !=null) {
            System.out.println("Files that were deleted:");
            deleteFile.forEach((k) ->
            {
                System.out.println(k);
            });
        }
    }

    private void loadRepositoryToOurObjects(String i_RepositoryPath) {
        try {
            if (m_MyMagit.isRepositoryExists(i_RepositoryPath)) {
                if (m_MyMagit.getM_Repositories() != null) {
                    m_MyMagit.clearRepository();
                }
                m_MyMagit.spanHeadBranchToOurObjects(i_RepositoryPath);
                m_MyMagit.spreadAllBranchesIntoOurObjects();
                m_MyMagit.spanAllNonPointedCommitsToOurObjects();
            }
            else {
                System.out.println("Can't find repository on that location!");
            }
        } catch (Exception e) {
            System.out.println("Can't find repository on that location!");
        }
    }

    private void ShowCurrentCommitFiles() {
        List<String> itemsToDisplay = new ArrayList<>();

        if((m_MyMagit.getM_Repositories() == null) || (m_MyMagit.getM_Repositories().getM_Branches().size() == 0) ) {
            System.out.println("There is no current commit!Can't do this action!");
        }
        else {
            try {
                m_MyMagit.ShowCurrentCommitFilesFromRepository(itemsToDisplay);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        printList(itemsToDisplay);
    }

    private void listAvailableBranches() {
        List<String> itemsToDisplay = new ArrayList<>();
        try{
            m_MyMagit.isRepositoryNull();
           m_MyMagit.readAllBranches(itemsToDisplay);
            printList(itemsToDisplay);
        }
        catch (NullPointerException ex)
        {
            System.out.println(ex.getMessage());
        }
        catch(IOException ex)
        {
            System.out.println(ex.toString());
        }
    }

    private void showCurrentBranchHistory(){
        try {
            m_MyMagit.isRepositoryNull();
            if(!m_MyMagit.isThereAnyBranch())
            {
                System.out.println("No branches available!");
                return;
            }
            List<String> result = m_MyMagit.showActiveBranchHistory();
            for (String str : result) {
                System.out.println(str);
            }
        }
        catch (NullPointerException ex)
        {
            System.out.println(ex.getMessage());
            return;
        }
        catch (IOException e) {
            System.out.println("Wasn't able to find or to open the Head file!");
        }
    }

    private void creatNewBranch(){
        try {
            m_MyMagit.isRepositoryNull();
        }
        catch (NullPointerException ex)
        {
            System.out.println(ex.getMessage());
            return;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter new brunch name:");
        String brunchName = scanner.nextLine();
        while (m_MyMagit.getM_Repositories().getM_Branches().get(brunchName) != null) {
            System.out.println("this name is already exists ,please enter new brunch name:");
            brunchName = scanner.next();
        }
        try {
            m_MyMagit.createBrunch(brunchName);
            System.out.println("Would you like to checkout to the new branch?(Yes/No)"); //Bonus part
            String inputFroUser = scanner.nextLine();
            if(inputFroUser.toLowerCase().equals("yes")){
                if(m_MyMagit.checkIfThereAreOpenChanges())
                {
                    System.out.println("There are open changes! can't peform checkout at the moment!");
                }
                else
                {
                    m_MyMagit.changeTheActiveBranchActivity(brunchName);
                    m_MyMagit.spanHeadBranchToOurObjects(m_MyMagit.getM_Repositories().getM_Location());
                    m_MyMagit.spreadAllBranchesIntoOurObjects();
                    m_MyMagit.spanAllNonPointedCommitsToOurObjects();
                    Deleter.deleteDir(m_MyMagit.getM_Repositories().getM_Location() + "/WC");
                    Creator.createWC(m_MyMagit);
                }
            }

        } catch (IOException e) {
            System.out.println("Wasn't able to write into the request file!Please try again.");
            return;
        }catch (NullPointerException ex){
            System.out.println(ex.getMessage());
            return;
        }
    }

    private void deleteBrunch(){
        Scanner scanner = new Scanner(System.in);
        try {
            m_MyMagit.isRepositoryNull();
            if(!m_MyMagit.isThereAnyBranch())
            {
                System.out.println("No branches available!");
                return;
            }
            System.out.println("Please enter brunch name:");
            String brunchName = scanner.next();
            m_MyMagit.deleteBrunch(brunchName);
        }
        catch (FileNotFoundException | NullPointerException ex)
        {
            System.out.println(ex.getMessage());
        } catch (IOException e) {
            System.out.println("Wasn't able to delete the requested file!Please try again.");
        }
    }

    private void checkOutBranch(){
        try {
            m_MyMagit.isRepositoryNull();
            if(!m_MyMagit.isThereAnyBranch())
            {
                System.out.println("No branches available!");
                return;
            }
        }
        catch (NullPointerException ex)
        {
            System.out.println(ex.getMessage());
            return;
        }
        Scanner scanner = new Scanner(System.in);
        List<String> allBranches = m_MyMagit.getAllBranchesToString();
        String selectedBranch = displayTheBranches(allBranches);
        boolean isOpenChange = m_MyMagit.checkIfThereAreOpenChanges();

        if(isOpenChange)
        {
            System.out.println("There are some unsaved changes , would you like to save them to your current repository?(Yes/No)");
            String userChoise = scanner.next();
            if(userChoise.toLowerCase().equals("yes")){
                m_MyMagit.createCommit();
            }
        }

        try{
            m_MyMagit.changeTheActiveBranchActivity(selectedBranch);
            m_MyMagit.spanHeadBranchToOurObjects(m_MyMagit.getM_Repositories().getM_Location());
            m_MyMagit.spreadAllBranchesIntoOurObjects();
            m_MyMagit.spanAllNonPointedCommitsToOurObjects();
        }
        catch (Exception ex)
        {
            System.out.println("File not found on that Location!");
            System.out.println(ex.getMessage());
            return;
        }
        try {
            Deleter.deleteDir(m_MyMagit.getM_Repositories().getM_Location() + "/WC");
        }
        catch (Exception ex){
            System.out.println(ex.getMessage());
            return;
        }
        Creator.createWC(m_MyMagit);
    }

    private void initializeNewRepository(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter repository location: ");
        String location = scanner.nextLine();
        System.out.println("Please enter repository name:");
        String name = scanner.nextLine();
        try {
            m_MyMagit.initializeNewRepository(location, name);
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
        }

    }

    private void resetActiveBranch(){
        try{
            m_MyMagit.isRepositoryNull();
        }
        catch (NullPointerException ex)
        {
            System.out.println(ex.getMessage());
            return;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter new commit Sha1");
        String commitSha1 = scanner.nextLine();
        String inputFromUser;
        try
        {
            if(!m_MyMagit.isThereAnyBranch())
            {
                System.out.println("No branches available!");
                return;
            }
            m_MyMagit.checkIfFileExistsInObjectsBySha1(commitSha1);
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            return;
        }

        if(m_MyMagit.checkIfThereAreOpenChanges())
        {
            System.out.println("There are open changes on your working copy,Would you like to continue?(Yes/No)");
            inputFromUser = scanner.nextLine();
            if(!(inputFromUser.toLowerCase().equals("yes")))
            {
                System.out.println("Forwarding you to main menu");
            }
        }
        try {
            m_MyMagit.resetActiveBranch(commitSha1);
            Deleter.deleteDir(m_MyMagit.getM_Repositories().getM_Location() + "/WC");
            Creator.createWC(m_MyMagit);
        }
        catch (IOException ex)
        {
            System.out.println(ex.getMessage());
        }
    }

    private void printList(List<String> listToDisplay){
        for (String str : listToDisplay){
            System.out.println(str);
        }
    }

    private String displayTheBranches(List<String> i_AllBranches){
        Scanner scanner = new Scanner(System.in);
        String selecteBranchFromUser = null;
        boolean isValidInput = false ;
        while(!isValidInput) {
            System.out.println("Please enter the wanted Branch name:");
            for (String str : i_AllBranches) {
                System.out.println(str);
            }
            selecteBranchFromUser = scanner.nextLine();
            for (String str : i_AllBranches) {
                if (str.equals(selecteBranchFromUser)) {
                    isValidInput = true;
                    break;
                }
            }
            if (!isValidInput) {
                System.out.println("Branch can't be found!");
            }
        }
        return selecteBranchFromUser;
    }


    private String getInputStringFromTheUser(String i_Message, String i_ErrorMessage) {
        Scanner scanner = new Scanner(System.in);
        String stringFromUser;

        System.out.println(i_Message);
        stringFromUser = scanner.nextLine();

        while (stringFromUser.isEmpty()) {
            System.out.println(i_ErrorMessage);
            stringFromUser = scanner.nextLine();
        }

        return stringFromUser;
    }

    private int getIntInputFromUser() {
        int userIntegerInput = 0;
        boolean validInput = false;
        Scanner scanner = new Scanner(System.in);

        do {
            try {
                userIntegerInput = scanner.nextInt();
                if (userIntegerInput >= 0 && userIntegerInput < eMenuOption.values().length) {
                    validInput = true;
                } else {
                    System.out.println("Invalid input! Your choice is out of range, try again");
                }
            } catch (InputMismatchException exception) {
                System.out.println("Invalid input! Please enter a number");
                scanner.nextLine();
            }
        } while (!validInput);

        return userIntegerInput;
    }

    private void commitFunction(){

        try{
            m_MyMagit.isRepositoryNull();
            m_MyMagit.createCommit();
        }
        catch (NullPointerException ex)
        {
            System.out.println(ex.getMessage());
            return;
        }

    }

    private void saveRepositoryToXML(){
        String xmlFullPath;

        if (this.m_MyMagit.getM_Repositories() != null) {
            xmlFullPath = getInputStringFromTheUser("Please enter the name of the xml file (as a full path):",
                    "Invalid input! You can't enter empty path, try again:");
            Path xmlPath = null;

            try {
                xmlPath = Paths.get(xmlFullPath);
            } catch (InvalidPathException ipe) {
                System.out.println("Not a valid path!");
            }

            if (isFileXML(xmlFullPath)) {
                try {
                    m_MyMagit.SaveRepositoryToXml(xmlFullPath);
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            } else {
                System.out.println("Not a valid path!");
            }
        }
        else
        {
            System.out.println("There is no loaded repository!");
        }
    }

    public static boolean isFileXML(String i_FullPath){
        return i_FullPath.endsWith(".xml");
    }

}


