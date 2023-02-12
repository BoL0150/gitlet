package gitlet;



import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author BoLee
 */
public class Commit implements Serializable {
    /** The message of this Commit. */
    private String message;
    private List<String> parentCommitID;
    // the input of sha1 function must be string
    private String commitTime;
    private HashMap<String,String>filePathToBlobId;
    // private HashMap<String,String>blobIdToFilePath;
    private String commitID;
    private boolean isMergeCommit = false;
    public HashMap<String, String> getFilePathToBlobId() {
        return filePathToBlobId;
    }
    public String getMessage() {
        return message;
    }

    public String getCommitTime() {
        return commitTime;
    }

    public List<String> getParentCommitID() {
        return parentCommitID;
    }
    public boolean isInitialCommit(){
        return parentCommitID.size() == 0;
    }
    public String getCommitID(){
        return commitID;
    }
    public String getBlobIdOfFilePath(String filePath){
        return filePathToBlobId.get(filePath);
    }
    // when .git init,invoke this method to create a
    // initial commit and save the commit object to .git/object
    public Commit() throws IOException {
        message = "initial commit";
        // initial commit's parentCommitID's size is 0
        parentCommitID = new LinkedList<String>();
        filePathToBlobId = new HashMap<String, String>(){};
        commitTime = formatDateToString(new Date(0));
        commitID = generateCommitID();
        isMergeCommit = false;
    }
    public Commit(String message,boolean isMergeCommit,String otherParentCommitID) throws IOException {
        this.message = message;
        this.isMergeCommit = isMergeCommit;
        // not a merge commit
        setParentCommitId(isMergeCommit,otherParentCommitID);
        commitTime = formatDateToString(new Date());
        updateTheCommitReferenceToBlob();
        commitID = generateCommitID();
    }
    public Commit(String message) throws IOException {
        this(message, false,"");
    }
    public void printCommit(){
        System.out.println("===");
        System.out.println("commit "+commitID);
        if(isMergeCommit){
            String firstParentCommitID = parentCommitID.get(0);
            String secondParentCommitID = parentCommitID.get(1);
            System.out.println("Merge: "+firstParentCommitID.substring(0,7)+" "+secondParentCommitID.substring(0,7));
        }
        System.out.println("Date: "+commitTime);
        System.out.println(message);
        System.out.println();
    }
    private String formatDateToString(Date date){
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }
    public boolean isThereIdenticalFileTo(String filePath, String blobID){
        // if two blobs are the same,filePath must be the same
        return filePathToBlobId.containsKey(filePath) && filePathToBlobId.get(filePath).equals(blobID);
    }
    public boolean isThereSameFileNameTo(String filePath){
        return filePathToBlobId.containsKey(filePath);
    }
    // the parent of  both mergeCommit and ordinary commit are current Commit
    private void updateTheCommitReferenceToBlob(){
        Index index = Utils.readObject(Repository.INDEX,Index.class);
        if(index.getAdditionalIndex().isEmpty() && index.getRemovalIndex().isEmpty())
            Utils.exitWithError("No changes added to the commit.");
        // copy parent commit's blob
        Commit parentCommit = Repository.getObject(parentCommitID.get(0),Commit.class);
        filePathToBlobId = parentCommit.getFilePathToBlobId();
        // filePath to blobId
        HashMap<String,String> additionIndex = index.getAdditionalIndex();
        for(Map.Entry<String,String> entry : additionIndex.entrySet()){
            String filePath = entry.getKey();
            String blobId = entry.getValue();
            // if name is the same,replace it ;or just add a new mapping
            filePathToBlobId.put(filePath, blobId);
        }
        // filePath to blobId
        HashMap<String,String> removalIndex = index.getRemovalIndex();
        for(Map.Entry<String,String> entry : removalIndex.entrySet()){
            String filePath = entry.getKey();
            filePathToBlobId.remove(filePath);
        }
    }
    private void setParentCommitId(boolean isMerge,String otherParentCommitID){
        // active branch
        File branchFile = Repository.getCurrentBranchFile();
        String firstParentCommitId = Utils.readContentsAsString(branchFile);
        String secondParentCommitId = new String();
        if(isMerge)secondParentCommitId = otherParentCommitID;
        else secondParentCommitId = firstParentCommitId;
        this.parentCommitID = new LinkedList<String>(){};
        parentCommitID.add(firstParentCommitId);
        parentCommitID.add(secondParentCommitId);
    }
    public void saveCommit() throws IOException {
        File commitObjectFile = Repository.createObjectFile(commitID);
        Utils.writeObject(commitObjectFile,this);
        // update branchFile to point to the newest commit
        File branchFile = Repository.getCurrentBranchFile();
        Utils.writeContents(branchFile,commitID);
        // update commit record in order to implement global-log and find
        CommitRecord commitRecord = Utils.readObject(Repository.COMMITS_RECORD,CommitRecord.class);
        commitRecord.add(this);
        Utils.writeObject(Repository.COMMITS_RECORD,commitRecord);
    }

    // use commit's content to generate the commit id
    private String generateCommitID(){
        // the input of sha1 function must be String
        return Utils.sha1(message,parentCommitID.toString(),filePathToBlobId.toString(),commitTime);
    }

}
