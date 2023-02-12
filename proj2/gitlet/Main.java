package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author BoLee
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {
        int argNum = args.length;
        if(argNum == 0) Utils.exitWithError("Please enter a command.");
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                Repository.repositorySetup();
                break;
            case "add":
                // handle the `add [filename]` command
                String filePath = args[1];
                Repository.addFile(filePath);
                break;
            case "commit":
                String message = args[1];
                Repository.commit(message);
                break;
            case "rm":
                Repository.rmFile(args[1]);
                break;
            case "log":
                Repository.printLog();
                break;
            case "global-log":
                Repository.printGlobalLog();
                break;
            case "find":
                Repository.find(args[1]);
                break;
            case "status":
                Repository.printStatus();
                break;
            case "checkout":
                String secondArg = args[1];
                if(argNum == 2){
                    Repository.checkoutFromBranch(secondArg);
                }else if(argNum == 3 && secondArg.equals("--")) {
                    Repository.checkoutTheFileFromLastestCommit(args[2]);
                }else if(argNum == 4 && args[2].equals("--")){
                    String forthArg = args[3];
                    Repository.checkoutTheFileFromCommitOf(secondArg,forthArg);
                }else{
                    Utils.exitWithError("Incorrect operands.");
                }
                break;
            case "branch":
                Repository.createBranch(args[1]);
                break;
            case "rm-branch":
                Repository.removeBranch(args[1]);
                break;
            case "reset":
                Repository.resetToCommit(args[1]);
                break;
            case "merge":
                Repository.merge(args[1]);
                break;
            default:
                Utils.exitWithError("No command with that name exists.");
                break;
        }
    }
}
