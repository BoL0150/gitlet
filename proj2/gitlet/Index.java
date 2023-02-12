package gitlet;

import javax.print.DocFlavor;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Index implements Serializable {
    // filePath to blobId
    private HashMap<String,String>additionalIndex = new HashMap<String, String>();
    private HashMap<String,String>removalIndex = new HashMap<String, String>();
    // blobid to filePath
    // private HashMap<String,String>reverseAdditonalIndex = new HashMap<String, String>();
    // private HashMap<String,String>reverseRemovalIndex = new HashMap<String, String>();
    public void printStagedFile(){
        for(Map.Entry<String,String> entry : additionalIndex.entrySet()){
            System.out.println(entry.getKey());
        }
    }
    public boolean isEmpty(){
        return additionalIndex.isEmpty() && removalIndex.isEmpty();
    }
    public void printRemovedFile(){
        for(Map.Entry<String,String> entry : removalIndex.entrySet()){
            System.out.println(entry.getKey());
        }
    }
    public void addFileToAdditionIndex(String filePath,String blobId){
        additionalIndex.put(filePath,blobId);
        // reverseAdditonalIndex.put(blobId,filePath);
    }
    public void addFileToRemovalIndex(String filePath,String blobId){
        removalIndex.put(filePath,blobId);
        // reverseRemovalIndex.put(blobId,filePath);
    }
    // public void removeFromAdditionIndex(String blobId){
    //     // String filePath = reverseAdditonalIndex.get(blobId);
    //     // reverseAdditonalIndex.remove(blobId);
    //     additionalIndex.remove(filePath);
    // }
    public void removeFromAdditionIndexAccordingToFilePath(String filePath){
        //  String blobId = additionalIndex.get(filePath);
        // reverseAdditonalIndex.remove(blobId);
        additionalIndex.remove(filePath);
    }
    public void removeFromRemovalIndexAccordingToFilePath(String filePath){
        removalIndex.remove(filePath);
    }
    public HashMap<String, String> getAdditionalIndex(){
        return additionalIndex;
    }
    public HashMap<String, String> getRemovalIndex(){
        return removalIndex;
    }
    // public boolean doesAdditionalIndexHasIdenticalFileTo(String blobId){
    //     return reverseAdditonalIndex.containsKey(blobId);
    // }
    public boolean doesAdditionalIndexHasSameFileNameTo(String filePath){
        return additionalIndex.containsKey(filePath);
    }
    public boolean doesRemovalIndexHasSameFileNameTo(String filePath){
        return removalIndex.containsKey(filePath);
    }
    public void clear(){
        additionalIndex = new HashMap<String, String>(){};
        // reverseAdditonalIndex = new HashMap<String, String>(){};
        removalIndex = new HashMap<String, String>(){};
        // reverseRemovalIndex = new HashMap<String, String>(){};
    }
}
