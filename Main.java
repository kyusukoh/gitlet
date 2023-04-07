package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Kyusuk Oh
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {
        //When empty args
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        if (firstArg.equals("init")) {
            Repository repository = new Repository(); //create a new repo
            repository.init(); //initiate repo
            Utils.writeContents(Repository.REPO, Utils.serialize(repository));
            return;
        } else if (!Repository.REPO.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        } else {
            Repository repository = Utils.readObject(Repository.REPO, Repository.class);
            if (firstArg.equals("add")) {
                repository.add(args[1]);
                Utils.writeContents(Repository.REPO, Utils.serialize(repository));
                return;
            } else if (firstArg.equals("rm")) {
                repository.rm(args[1]);
                Utils.writeContents(Repository.REPO, Utils.serialize(repository));
                return;
            } else if (firstArg.equals("commit")) {
                repository.commit(args[1]);
                Utils.writeContents(Repository.REPO, Utils.serialize(repository));
            } else if (firstArg.equals("checkout")) {
                if (args.length == 2) {
                    repository.checkoutBranch(args[1]);
                    Utils.writeContents(Repository.REPO, Utils.serialize(repository));
                } else if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        System.out.println("Incorrect operands.");
                    } else {
                        repository.checkoutFile(args[2]);
                        Utils.writeContents(Repository.REPO, Utils.serialize(repository));
                    }
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                    } else {
                        repository.checkoutCommit(args[1], args[3]);
                        Utils.writeContents(Repository.REPO, Utils.serialize(repository));
                    }
                }
            } else if (firstArg.equals("branch")) {
                if (args[1] == null) {
                    System.out.println("No branch name");
                } else {
                    repository.branch(args[1]);
                    Utils.writeContents(Repository.REPO, Utils.serialize(repository));
                }
            } else if (firstArg.equals("rm-branch")) {
                repository.rmBranch(args[1]);
                Utils.writeContents(Repository.REPO, Utils.serialize(repository));
            } else if (firstArg.equals("log")) {
                repository.log();
            } else if (firstArg.equals("status")) {
                repository.status();
            } else if (firstArg.equals("global-log")) {
                repository.globalLog();
            } else if (firstArg.equals("find")) {
                repository.find(args[1]);
            } else if (firstArg.equals("merge")) {
                repository.merge(args[1]);
                Utils.writeContents(Repository.REPO, Utils.serialize(repository));
            } else if (firstArg.equals("reset")) {
                repository.reset(args[1]);
                Utils.writeContents(Repository.REPO, Utils.serialize(repository));
            } else {
                System.out.println("No command with that name exists.");
            }
        }
    }
}
