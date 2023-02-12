package gitlet;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author BoLee
 */
public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJECTS_DIR = join(GITLET_DIR,"objects");
    public static final File heads = join(GITLET_DIR,"refs","heads");
    public static final File MASTER = join(heads,"master");
    // HEAD point to the active branch
    public static final File HEAD = join(GITLET_DIR,"HEAD");
    public static final File COMMITS_RECORD = join(GITLET_DIR,"commits_record");
    public static final File INDEX = join(GITLET_DIR,"INDEX");

    /**
     * create .gitlet repository
     */
    public static void repositorySetup() throws IOException {
        if(GITLET_DIR.exists()){
            Utils.exitWithError("A Gitlet version-control system already exists in the current directory.");
        }
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        setupCommitsRecord();
        setupIndex();
        initMasterBranch();
        // if initialCommit ,there is no need to apply commit massege to invoke the defualt constructor
        Commit commit = new Commit();
        commit.saveCommit();
    }
    public static void setupCommitsRecord() throws IOException {
        COMMITS_RECORD.createNewFile();
        CommitRecord commitRecord = new CommitRecord();
        Utils.writeObject(COMMITS_RECORD,commitRecord);
    }
    public static void commit(String message) throws IOException {
        commit(message,false,"");
    }
    public static void commit(String message,boolean isMerge,String otherParentCommitID) throws IOException {
        if(message.isEmpty()) {
            Utils.exitWithError("Please enter a commit message.");
        }
        //if(isMerge) System.out.println("FFFFFFFFFFFFFFFFFFFFFFFF");
        Commit commit = new Commit(message,isMerge,otherParentCommitID);
        clearIndex();
        commit.saveCommit();
    }
    public static void clearIndex(){
        Index index = Utils.readObject(INDEX,Index.class);
        index.clear();
        Utils.writeObject(INDEX,index);
    }
    private static void setupIndex() throws IOException {
        INDEX.createNewFile();
        Index index = new Index();
        Utils.writeObject(INDEX,index);
    }
    private static void initMasterBranch() throws IOException {
        // mkdirs is able to create any nonexistent parent folder
        heads.mkdirs();
        MASTER.createNewFile();
        HEAD.createNewFile();
        // HEAD file contain the name of the active branch
        Utils.writeContents(HEAD,"master");
    }
    // git add filePath
    public static void addFile(String filePath) throws IOException {
        File fileToBeAdded = join(CWD,filePath);
        if(!fileToBeAdded.exists())exitWithError("File does not exist.");
        Blob blob = new Blob(fileToBeAdded,filePath);
        blob.saveBlobToAdditionIndex();
        blob.saveBlobToFile();
    }
    // git rm filePath
    public static void rmFile(String filePath) throws IOException {
        File fileToBeRemoved = join(CWD,filePath);
        Blob blob = new Blob(fileToBeRemoved,filePath);
        blob.saveBlobToRemovalIndex();
    }
    // create file in the .git/object for commit,blob and tree object
    public static File createObjectFile(String objectID) throws IOException {
        String firstTwoCharOfID = objectID.substring(0,2);
        String remainId = objectID.substring(2);
        // the directory whose name is the first two char of object id
        File objectDir = Utils.join(OBJECTS_DIR,firstTwoCharOfID);
        if(!objectDir.exists())objectDir.mkdir();
        // the file whose name is the remain char of the object id
        File objectFile = Utils.join(objectDir,remainId);
        objectFile.createNewFile();
        return objectFile;
    }
    public static File getCurrentBranchFile(){
        String activeBranch = Utils.readContentsAsString(HEAD);
        File branchFile = Utils.join(heads,activeBranch);
        return branchFile;
    }
    public static <T extends Serializable> T getObject(String objectID, Class<T> expectedClass){
        String firstTwoCharOfID = objectID.substring(0,2);
        String remainId = objectID.substring(2);
        File objectFile = join(OBJECTS_DIR,firstTwoCharOfID,remainId);
        if(expectedClass == Commit.class && !objectFile.exists())Utils.exitWithError("No commit with that id exists.");
        T object = Utils.readObject(objectFile,expectedClass);
        return object;
    }
    public static Commit getCurrentCommit(){
        File branchFile = getCurrentBranchFile();
        String currentCommitId = Utils.readContentsAsString(branchFile);
        return getObject(currentCommitId,Commit.class);
    }
    public static void printLog(){
        Commit currentCommit = getCurrentCommit();
        // initial commit's parentCommitID's size is 0
        while(!currentCommit.isInitialCommit()){
            currentCommit.printCommit();
            String firstParentCommitID = currentCommit.getParentCommitID().get(0);
            currentCommit = getObject(firstParentCommitID,Commit.class);
        }
        currentCommit.printCommit();
    }
    public static void printGlobalLog(){
        CommitRecord commitRecord = Utils.readObject(COMMITS_RECORD,CommitRecord.class);
        commitRecord.printCommitRecord();
    }
    public static void find(String message){
        CommitRecord commitRecord = Utils.readObject(COMMITS_RECORD,CommitRecord.class);
        commitRecord.findSameMessageCommitID(message);
    }
    public static void printStatus(){
        if(!GITLET_DIR.exists())exitWithError("Not in an initialized Gitlet directory.");
        System.out.println("=== Branches ===");
        printBranches();
        System.out.println();
        Index index = Utils.readObject(Repository.INDEX,Index.class);
        System.out.println("=== Staged Files ===");
        index.printStagedFile();
        System.out.println();
        System.out.println("=== Removed Files ===");
        index.printRemovedFile();
        System.out.println();
        // extra credit
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }
    public static void printBranches(){
        String currentBranchName = Utils.readContentsAsString(HEAD);
        System.out.println("*"+currentBranchName);
        String[] branchList = heads.list();
        for(String branchName : branchList){
            if(!branchName.equals(currentBranchName)){
                System.out.println(branchName);
            }
        }
    }
    public static void checkoutTheFileFromLastestCommit(String filePath){
        String currentCommitID = getCurrentCommit().getCommitID();
        checkoutTheFileFromCommitOf(currentCommitID,filePath);
    }
    public static void checkoutTheFileFromCommitOf(String commitID,String filePath){
        Commit commit = getObject(commitID,Commit.class);
        if(!commit.isThereSameFileNameTo(filePath))
            Utils.exitWithError("File does not exist in that commit.");
        String blobID = commit.getFilePathToBlobId().get(filePath);
        writeBlobIntoWorkingDir(blobID,filePath);
    }
    public static void writeBlobIntoWorkingDir(String blobID,String filePath){
        Blob blob = getObject(blobID,Blob.class);
        byte[] blobContent = blob.getFileContent();
        // if filePath already exist,overwrite it;or create a new file and the write into it
        File newFile = join(CWD,filePath);
        Utils.writeContents(newFile,blobContent);
    }
    public static void checkoutFromBranch(String branchName){
        File branchFile = join(heads,branchName);
        String currentBranchName =  getCurrentBranchFile().getName();
        if(!branchFile.exists())Utils.exitWithError("No such branch exists.");
        if(currentBranchName.equals(branchName))Utils.exitWithError("No need to checkout the current branch.");
        String checkoutCommitID = Utils.readContentsAsString(branchFile);
        restoreWorkingDirToCommit(checkoutCommitID);
        updateHEAD(branchName);
    }
    public static void resetToCommit(String checkoutCommitID){
        restoreWorkingDirToCommit(checkoutCommitID);
        updateCurrentBranchFileTo(checkoutCommitID);
    }
    private static void restoreWorkingDirToCommit(String checkoutCommitID){
        Commit checkoutCommit = getObject(checkoutCommitID,Commit.class);
        Commit currentCommit = getCurrentCommit();
        HashMap<String,String> checkoutCommitBlobs = checkoutCommit.getFilePathToBlobId();
        HashMap<String,String> currentCommitBlobs = currentCommit.getFilePathToBlobId();
        // first we need to traverse the blobs in the current branch commit to find fileName not
        // exist in the checkoutCommit and delete from working directory
        for(Map.Entry<String,String> entry : currentCommitBlobs.entrySet()){
            String filePath = entry.getKey();
            if(!checkoutCommit.isThereSameFileNameTo(filePath)){
                Utils.restrictedDelete(filePath);
            }
        }
        // then traverse the blobs in the checkout commit,for the files that are not in the commit of the current branch,
        // but are in the working directory,output an error message(as the name of the file in the working directory is same
        // to the one in the checkout commit,it gonna be overwritten by the same name file in the checkout commit.
        // Therefore,in order to avoid information loss,gitlet will report an error)
        for(Map.Entry<String,String> entry : checkoutCommitBlobs.entrySet()){
            String filePath = entry.getKey();
            String blobID = entry.getValue();
            File file = Utils.join(CWD,filePath);
            if(!currentCommit.isThereSameFileNameTo(filePath) && file.exists()){
                Utils.exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
            }
            writeBlobIntoWorkingDir(blobID,filePath);
        }
        clearIndex();
    }
    public static void updateHEAD(String branchName){
        Utils.writeContents(HEAD,branchName);
    }
    public static void updateCurrentBranchFileTo(String commitID){
        File currentBranchFile = getCurrentBranchFile();
        Utils.writeContents(currentBranchFile,commitID);
    }
    public static void updateBranchFileTo(String branchName,String commitID){
        File branchFile = join(heads,branchName);
        Utils.writeContents(branchFile,commitID);
    }
    public static void createBranch(String branchName){
        File newBranchFile = Utils.join(heads,branchName);
        if(newBranchFile.exists())Utils.exitWithError("A branch with that name already exists.");
        String commitID = getCurrentCommit().getCommitID();
        Utils.writeContents(newBranchFile,commitID);
    }
    private static File getBranchFile(String branchName){
        File branchFile = join(heads,branchName);
        if(!branchFile.exists())Utils.exitWithError("A branch with that name does not exist.");
        return branchFile;
    }
    public static void removeBranch(String branchName){
        File removedBranchFile = getBranchFile(branchName);
        if(removedBranchFile.getName().equals(getCurrentBranchFile().getName()))Utils.exitWithError("Cannot remove the current branch.");
        removedBranchFile.delete();
    }
    public static Commit getFrontCommitOfBranch(String branchName){
        File branchFile = getBranchFile(branchName);
        String commitID = readContentsAsString(branchFile);
        return getObject(commitID,Commit.class);
    }
    private static Commit BFS(Commit commit,LinkedList<Commit>commitsQueue,HashSet<String>curCommitVisited,boolean isCommit2,HashSet<String>prevCommitVisited){
        commitsQueue.addLast(commit);
        curCommitVisited.add(commit.getCommitID());
        while (!commitsQueue.isEmpty()){
            Commit curCommit = commitsQueue.poll();
            if(isCommit2 && prevCommitVisited.contains(curCommit.getCommitID()))return curCommit;
            if(curCommit.isInitialCommit())continue;
            for(int i = 0;i <= 1;i++){
                String parentCommitID = curCommit.getParentCommitID().get(i);
                if(curCommitVisited.contains(parentCommitID))continue;
                Commit parentCommit = getObject(parentCommitID,Commit.class);
                commitsQueue.addLast(parentCommit);
                curCommitVisited.add(parentCommitID);
            }
        }
        return null;
    }
    private static Commit getSplitPoint(Commit commit1,Commit commit2){
        HashSet<String>commit1Visited = new HashSet<String>();
        LinkedList<Commit>commitsQueue = new LinkedList<Commit>();
        BFS(commit1,commitsQueue,commit1Visited,false,null);
        commitsQueue = new LinkedList<Commit>();
        HashSet<String>commit2Visited = new HashSet<String>();
        return BFS(commit2,commitsQueue,commit2Visited,true,commit1Visited);
    }
    // use blobID as the totalMap's key,as the same filePath may have different BlobID in these three Map
    private static void addToTotalBlobIDToFilePath(HashMap<String,String> totalBlobIDToFilePath,HashMap<String,String> filePathToBlobs){
        for(Map.Entry<String,String> entry : filePathToBlobs.entrySet()){
            String filePath = entry.getKey();
            String blobID = entry.getValue();
            totalBlobIDToFilePath.put(blobID,filePath);
        }
    }
    private static HashMap<String,String> getTotalBlobIDToFilePath(Commit commit1,Commit commit2,Commit commit3){
        // use blobID as the totalMap's key,as the same filePath may have different BlobID in these three Map
        HashMap<String,String> totalBlobIDToFilePath = new HashMap<String, String>();
        HashMap<String,String> firstCommitFilePathToBlobs = commit1.getFilePathToBlobId();
        HashMap<String,String> secondCommitFilePathToBlobs = commit2.getFilePathToBlobId();
        HashMap<String,String> thirdCommitFilePathToBlobs = commit3.getFilePathToBlobId();
        addToTotalBlobIDToFilePath(totalBlobIDToFilePath,firstCommitFilePathToBlobs);
        addToTotalBlobIDToFilePath(totalBlobIDToFilePath,secondCommitFilePathToBlobs);
        addToTotalBlobIDToFilePath(totalBlobIDToFilePath,thirdCommitFilePathToBlobs);
        //// TODO:remove
        //for(Map.Entry<String,String> entry : totalBlobIDToFilePath.entrySet()){
        //    String filePath = entry.getValue();
        //    System.out.println(filePath);
        //}
        return totalBlobIDToFilePath;
    }
    public static void merge(String branchName) throws IOException {
        Index index = readObject(INDEX,Index.class);
        if(!index.isEmpty())exitWithError("You have uncommitted changes.");
        if(branchName.equals(getCurrentBranchFile().getName()))exitWithError("Cannot merge a branch with itself.");
        Commit currentCommit = getCurrentCommit();
        Commit otherCommit = getFrontCommitOfBranch(branchName);
        Commit splitPoint = getSplitPoint(currentCommit,otherCommit);
        if(currentCommit.getCommitID().equals(splitPoint.getCommitID())){
            checkoutFromBranch(branchName);
            updateBranchFileTo(branchName,otherCommit.getCommitID());
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        if(otherCommit.getCommitID().equals(splitPoint.getCommitID())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        HashMap<String,String> totalBlobIDToFilePath = getTotalBlobIDToFilePath(splitPoint,currentCommit,otherCommit);
        boolean isConflicted = false;
        for(Map.Entry<String,String> entry : totalBlobIDToFilePath.entrySet()){
            String blobID = entry.getKey();
            String filePath = entry.getValue();
            String blobInSplitPoint = "",blobInOtherCommit = "",blobInCurrentCommit = "";
            if(splitPoint.isThereSameFileNameTo(filePath)) blobInSplitPoint = splitPoint.getBlobIdOfFilePath(filePath);
            if(otherCommit.isThereSameFileNameTo(filePath)) blobInOtherCommit = otherCommit.getBlobIdOfFilePath(filePath);
            if(currentCommit.isThereSameFileNameTo(filePath)) blobInCurrentCommit = currentCommit.getBlobIdOfFilePath(filePath);

            if (splitPoint.isThereSameFileNameTo(filePath) && !currentCommit.isThereSameFileNameTo(filePath) && !otherCommit.isThereSameFileNameTo(filePath)){
                // System.out.println(filePath + "1111111111111");
                continue;
            }
            if(!splitPoint.isThereSameFileNameTo(filePath) && currentCommit.isThereSameFileNameTo(filePath) && !otherCommit.isThereSameFileNameTo(filePath)){
                // System.out.println(filePath + "2222222222222");
                continue;
            }
            if(!splitPoint.isThereSameFileNameTo(filePath) && otherCommit.isThereSameFileNameTo(filePath) && !currentCommit.isThereSameFileNameTo(filePath)){
                if(join(CWD,filePath).exists())exitWithError("There is an untracked file in the way; delete it, or add and commit it first.");
                writeBlobIntoWorkingDir(blobID,filePath);
                index.addFileToAdditionIndex(filePath,blobID);
                // System.out.println(filePath + "33333333333333");
                continue;
            }
            // before using filePath to get blobID,we need to check whether the filePath exist in the Map
            if(splitPoint.isThereSameFileNameTo(filePath) && !otherCommit.isThereSameFileNameTo(filePath) &&
                    currentCommit.isThereSameFileNameTo(filePath) && blobInSplitPoint.equals(blobInCurrentCommit)){
                Utils.restrictedDelete(join(CWD,filePath));
                index.addFileToRemovalIndex(filePath,blobID);
                //System.out.println(filePath + "44444444444444444");
                continue;
            }
            if(splitPoint.isThereSameFileNameTo(filePath) && !currentCommit.isThereSameFileNameTo(filePath) &&
                    otherCommit.isThereSameFileNameTo(filePath) && blobInSplitPoint.equals(blobInOtherCommit)){
                //System.out.println(filePath + "5555555555555555555");
                continue;
            }
            if(splitPoint.isThereSameFileNameTo(filePath) && otherCommit.isThereSameFileNameTo(filePath) &&
                    currentCommit.isThereSameFileNameTo(filePath)){
                if(!blobInOtherCommit.equals(blobInSplitPoint) && blobInCurrentCommit.equals(blobInSplitPoint)){
                    // change the file in the working directory to the version in the otherCommit
                    writeBlobIntoWorkingDir(blobInOtherCommit,filePath);
                    index.addFileToAdditionIndex(filePath,blobInOtherCommit);
                 //   System.out.println(filePath + "6666666666666666");
                    continue;
                }
                if(blobInOtherCommit.equals(blobInSplitPoint) && !blobInCurrentCommit.equals(blobInSplitPoint)){
                  //  System.out.println(filePath + "777777777777777777");
                    continue;
                }
                if(!blobInOtherCommit.equals(blobInSplitPoint) && !blobInCurrentCommit.equals(blobInSplitPoint) && blobInCurrentCommit.equals(blobInOtherCommit)){
                   // System.out.println(filePath + "8888888888888888888");
                    continue;
                }
                if(!blobInOtherCommit.equals(blobInSplitPoint) && !blobInCurrentCommit.equals(blobInSplitPoint) && !blobInCurrentCommit.equals(blobInOtherCommit)){
                    mergeConflict(filePath,false, blobInCurrentCommit,false,blobInOtherCommit);
                    //System.out.println(filePath + "99999999999999999999");
                    isConflicted = true;
                    continue;
                }
            }
            if(splitPoint.isThereSameFileNameTo(filePath)){
                //System.out.println(filePath + "***********************");
                if(!currentCommit.isThereSameFileNameTo(filePath) && otherCommit.isThereSameFileNameTo(filePath) &&
                        !blobInOtherCommit.equals(blobInSplitPoint)){
                    isConflicted = true;

                    mergeConflict(filePath,true,"",false,blobInOtherCommit);
                }
                if(!otherCommit.isThereSameFileNameTo(filePath) && currentCommit.isThereSameFileNameTo(filePath) &&
                        !blobInCurrentCommit.equals(blobInSplitPoint)){
                    isConflicted = true;
                    mergeConflict(filePath,false,blobInCurrentCommit,true,"");
                }
                continue;
            }else if(currentCommit.isThereSameFileNameTo(filePath) && otherCommit.isThereSameFileNameTo(filePath) &&
                    !blobInCurrentCommit.equals(blobInOtherCommit)) {
                //System.out.println(filePath + "&&&&&&&&&&&&&&&&&&&");
                isConflicted = true;
                mergeConflict(filePath, false, blobInCurrentCommit, false, blobInOtherCommit);
            }
        }
        if(isConflicted) System.out.println("Encountered a merge conflict.");
        String currentBranchName = getCurrentBranchFile().getName();
        String commitMessage = "Merged " + branchName + " into " + currentBranchName;
        writeObject(INDEX,index);
        commit(commitMessage,true,otherCommit.getCommitID());
    }
    private static void mergeConflict(String conflictFileName,boolean curBlobEmpty,String currentBlobID,boolean otherBlobEmpty,String otherBlobID) throws IOException {
        File conflictFile = join(CWD,conflictFileName);
        String contentInCurrentBranch;
        String contentInOtherBranch;
        if(curBlobEmpty) contentInCurrentBranch = "";
        else contentInCurrentBranch = getObject(currentBlobID,Blob.class).getFileContentInString();
        if(otherBlobEmpty)contentInOtherBranch = "";
        else contentInOtherBranch = getObject(otherBlobID,Blob.class).getFileContentInString();
        writeContents(conflictFile,"<<<<<<< HEAD\n" + contentInCurrentBranch + "=======\n" + contentInOtherBranch + ">>>>>>>\n");
        addFile(conflictFileName);}
}

