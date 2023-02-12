package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class Blob implements Serializable {
    private String blobID;
    private byte[] fileContent;
    private String fileContentInString;
    private String filePath;
    // when using git add filePath command,invoke Blob constructor to create a Blob object
    // and save the blob object to .git/object and add the blobID to the index file
    public Blob(File fileToBeAdded,String filePath) throws IOException {
        // the file of git rm may not exist
        if(!fileToBeAdded.isFile()){
            fileContent = new byte[]{};
            fileContentInString = "";
        } else {
            fileContent = Utils.readContents(fileToBeAdded);
            fileContentInString = Utils.readContentsAsString(fileToBeAdded);
        }
        this.filePath = filePath;
        generateID();
    }

    public String getFileContentInString(){
        return fileContentInString;
    }
    public byte[] getFileContent() {
        return fileContent;
    }

    public void saveBlobToFile() throws IOException {
        File blobFile = Repository.createObjectFile(blobID);
        Utils.writeObject(blobFile,this);
    }
    public void saveBlobToRemovalIndex() throws IOException{
        File file = Utils.join(Repository.CWD,filePath);
        Index index = Utils.readObject(Repository.INDEX,Index.class);
        Commit currentCommit = Repository.getCurrentCommit();
        if(index.doesAdditionalIndexHasSameFileNameTo(filePath) && !currentCommit.isThereSameFileNameTo(filePath)){
            // do not remove file from working directory unless it is tacked by commit
            index.removeFromAdditionIndexAccordingToFilePath(filePath);
        }else if(currentCommit.isThereSameFileNameTo(filePath) && file.exists()){
            index.addFileToRemovalIndex(filePath,blobID);
            Utils.restrictedDelete(file);
        }else if(currentCommit.isThereSameFileNameTo(filePath) && !file.exists()){
            index.addFileToRemovalIndex(filePath,blobID);
        }else{
            Utils.exitWithError("No reason to remove the file.");
        }
        Utils.writeObject(Repository.INDEX,index);
    }
    public void saveBlobToAdditionIndex() throws IOException {
        Index index = Utils.readObject(Repository.INDEX,Index.class);
        // if the file to be added is identical to the current commit,
        // do not stage it, and remove it form the stageing area if it is already there(which means this file name is already in index)
        // this situation can happen when a file is changed,added,then changed back to it's
        // original version,and then to be added
        Commit currentCommit = Repository.getCurrentCommit();
        if(currentCommit.isThereIdenticalFileTo(filePath,blobID)){
            // Here we are supposed to remove file from index according to filePath rather than blobId
            if(index.doesAdditionalIndexHasSameFileNameTo(filePath)){
                index.removeFromAdditionIndexAccordingToFilePath(filePath);
            }
            if(index.doesRemovalIndexHasSameFileNameTo(filePath)){
                index.removeFromRemovalIndexAccordingToFilePath(filePath);
            }
            Utils.writeObject(Repository.INDEX,index);
            return;
        }
        // if the file is already staged,use the new blobId to overwrite
        // it's previous blobid in the index file
        index.addFileToAdditionIndex(filePath,blobID);
        // overwrite or create
        Utils.writeObject(Repository.INDEX,index);
    }
    // Note!!!!blob use fileContent and file path together to generate it's id!
    // if and only if two files' file content and path are all identical,these two files are same
    private void generateID(){
        blobID = Utils.sha1(fileContent,filePath);
    }
}
