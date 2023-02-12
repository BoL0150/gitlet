package gitlet;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/** maintain a commit record in order to implement global-log and find
 */
public class CommitRecord implements Serializable {
    private List<Commit>commitList = new LinkedList<Commit>();
    public void add(Commit commit){
        commitList.add(commit);
    }
    public void printCommitRecord(){
        for(Commit commit : commitList){
            commit.printCommit();
        }
    }
    public void findSameMessageCommitID(String message){
        boolean isFinded = false;
        for(Commit commit : commitList){
            if(commit.getMessage().equals(message)){
                isFinded = true;
                System.out.println(commit.getCommitID());
            }
        }
        if(!isFinded) System.out.println("Found no commit with that message.");
    }
}
