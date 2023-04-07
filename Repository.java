package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  @author Kyusuk Oh
 */
public class Repository implements Serializable {
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File REPOSITORY = Utils.join(GITLET_DIR, "repository");
    public static final File REPO = new File(".gitlet/repository/repo.txt");
    public static final File TO_ADD = Utils.join(GITLET_DIR, "to_add");
    public static final File ADDMAP = new File(".gitlet/to_add/add.txt");
    public static final File ADDLOG = new File(".gitlet/to_add/addlogmap.txt");
    public static final File TO_REMOVE = Utils.join(GITLET_DIR, "to_remove");
    public static final File REMOVEMAP = new File(".gitlet/to_remove/remove.txt");
    public static final File COMMITS = Utils.join(GITLET_DIR, "commits");
    public static final File COMMITMAP = new File(".gitlet/commits/commit.txt");
    public static final File TREE = Utils.join(GITLET_DIR, "tree");
    public static final File MAPS = new File(".gitlet/tree/maps.txt");

    public static void init() throws IOException {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control"
                    + " system already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_DIR.mkdir();
            REPOSITORY.mkdir();
            REPO.createNewFile();
            TO_ADD.mkdir();
            ADDLOG.createNewFile();
            ADDMAP.createNewFile();
            TO_REMOVE.mkdir();
            REMOVEMAP.createNewFile();
            COMMITS.mkdir();
            COMMITMAP.createNewFile();
            TREE.mkdir();
            MAPS.createNewFile();

            writeContents(ADDLOG, Utils.serialize(new HashMap<byte[], String>()));
            writeContents(ADDMAP, Utils.serialize(new HashMap<String, byte[]>()));
            writeContents(REMOVEMAP, Utils.serialize(new HashMap<String, byte[]>()));
            writeContents(COMMITMAP, Utils.serialize(new HashMap<String, byte[]>()));
            writeContents(MAPS, Utils.serialize(new Tree()));

            //make empty commit, update HEAD, and write in commits file
            Commit initialCommit = new Commit("initial commit", "", null);
            Tree tree = Utils.readObject(MAPS, Tree.class);

            HashMap<String, Commit> commitMap = tree.getCommitMap();
            Commit head = tree.getHEAD();
            HashMap<String, Commit> branches = tree.getBranches();
            String currentBranch = tree.getCurrentBranch();

            commitMap.put(initialCommit.getID(), initialCommit);
            head = initialCommit;
            branches.put("main", initialCommit);
            currentBranch = "main";

            tree.setCommitMap(commitMap);
            tree.setHEAD(head);
            tree.setBranches(branches);
            tree.setCurrentBranch(currentBranch);

            Utils.writeContents(COMMITMAP, Utils.serialize(commitMap));
            Utils.writeContents(MAPS, Utils.serialize(tree));
        }
    }

    public static void add(String fileName) {
        File fileToAdd = new File(fileName);
        if (!fileToAdd.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        Tree tree = readObject(MAPS, Tree.class);
        HashMap<String, byte[]> stagedAdd = tree.getStagedAdd();
        HashMap<String, byte[]> stagedRemove = tree.getStagedRemove();
        Commit head = tree.getHEAD();

        if (stagedRemove.containsKey(fileName)) {
            stagedRemove.remove(fileName);
        } else {
            if (head.getCommitted().containsKey(fileName)) {
                if (stagedAdd.containsKey(fileName)) {
                    if (stagedAdd.get(fileName).equals(Utils.readContents(fileToAdd))) {
                        return;
                    } else {
                        stagedAdd.put(fileName, Utils.readContents(fileToAdd));
                    }
                } else {
                    if (Arrays.equals(head.getCommitted().get(fileName),
                            Utils.readContents(fileToAdd))) {
                        return;
                    } else {
                        stagedAdd.put(fileName, Utils.readContents(fileToAdd));
                    }
                }
            } else {
                if (stagedAdd.containsKey(fileName)) {
                    if (stagedAdd.get(fileName).equals(Utils.readContents(fileToAdd))) {
                        return;
                    } else {
                        stagedAdd.put(fileName, Utils.readContents(fileToAdd));
                    }
                } else {
                    stagedAdd.put(fileName, Utils.readContents(fileToAdd));
                }
            }
        }

        tree.setStagedAdd(stagedAdd);
        tree.setStagedRemove(stagedRemove);
        tree.setHEAD(head);

        Utils.writeContents(REMOVEMAP, Utils.serialize(stagedRemove));
        Utils.writeContents(ADDMAP, Utils.serialize(stagedAdd));
        Utils.writeContents(MAPS, Utils.serialize(tree));
    }

    public static void rm(String fileName) {
        File fileToRemove = new File(fileName);

        Tree tree = readObject(MAPS, Tree.class);
        HashMap<String, byte[]> stagedAdd = tree.getStagedAdd();
        HashMap<String, byte[]> stagedRemove = tree.getStagedRemove();
        Commit head = tree.getHEAD();

        stagedAdd = readObject(ADDMAP, HashMap.class);
        stagedRemove = readObject(REMOVEMAP, HashMap.class);

        //if not in parent commit and not in current add
        if (!stagedAdd.containsKey(fileName) && !head.getCommitted().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        } else if (!head.getCommitted().containsKey(fileName) && !fileToRemove.exists()) {
            System.out.println("File does not exist.");
            return;
        } else if (head.getCommitted().containsKey(fileName)) {
            stagedRemove.put(fileName, head.getCommitted().get(fileName));
            Utils.restrictedDelete(fileName);
            if (stagedAdd.containsKey(fileName)) {
                stagedAdd.remove(fileName);
            }
        } else if (stagedAdd.containsKey(fileName)) {
            stagedAdd.remove(fileName);
        }

        tree.setStagedAdd(stagedAdd);
        tree.setStagedRemove(stagedRemove);
        tree.setHEAD(head);

        Utils.writeContents(MAPS, Utils.serialize(tree));
        Utils.writeContents(ADDMAP, Utils.serialize(stagedAdd));
        Utils.writeContents(REMOVEMAP, Utils.serialize(stagedRemove));
    }

    public static void commit(String message) {
        if (message.length() == 0) {
            System.out.println("Please enter a commit message.");
            return;
        } else {
            Tree tree = Utils.readObject(MAPS, Tree.class);
            HashMap<String, byte[]> stagedAdd = tree.getStagedAdd();
            HashMap<String, byte[]> stagedRemove = tree.getStagedRemove();
            HashMap<String, Commit> commitMap = tree.getCommitMap();
            Commit head = tree.getHEAD();
            HashMap<String, Commit> branches = tree.getBranches();
            String currentBranch = tree.getCurrentBranch();

            stagedAdd = Utils.readObject(ADDMAP, HashMap.class);
            stagedRemove = Utils.readObject(REMOVEMAP, HashMap.class);

            if (stagedAdd.isEmpty() && stagedRemove.isEmpty()) {
                System.out.println("No changes added to the commit.");
            } else {
                //make commit object
                Date now = new Date();
                SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
                String commitTime = timeFormat.format(now);
                Commit newCommit = new Commit(message, commitTime, head);

                //set HEAD
                head = newCommit;

                //set branch
                branches.remove(currentBranch);
                branches.put(currentBranch, newCommit);

                //save to commit.txt
                commitMap = Utils.readObject(COMMITMAP, HashMap.class);
                commitMap.put(newCommit.getID(), newCommit);

                //clear staging area
                stagedAdd.clear();
                stagedRemove.clear();
                Utils.writeContents(ADDMAP, Utils.serialize(stagedAdd));
                Utils.writeContents(REMOVEMAP, Utils.serialize(stagedRemove));
                Utils.writeContents(COMMITMAP, Utils.serialize(commitMap));
                //save tree
                tree.setStagedAdd(stagedAdd);
                tree.setStagedRemove(stagedRemove);
                tree.setCommitMap(commitMap);
                tree.setHEAD(head);
                tree.setBranches(branches);
                tree.setCurrentBranch(currentBranch);
                Utils.writeContents(MAPS, Utils.serialize(tree));
            }
        }
    }

    public static void mergeCommit(String message, Commit branchCommit) {
        Tree tree = Utils.readObject(MAPS, Tree.class);
        HashMap<String, byte[]> stagedAdd = tree.getStagedAdd();
        HashMap<String, byte[]> stagedRemove = tree.getStagedRemove();
        HashMap<String, Commit> commitMap = tree.getCommitMap();
        Commit head = tree.getHEAD();
        HashMap<String, Commit> branches = tree.getBranches();
        String currentBranch = tree.getCurrentBranch();

        stagedAdd = Utils.readObject(ADDMAP, HashMap.class);
        stagedRemove = Utils.readObject(REMOVEMAP, HashMap.class);

        if (stagedAdd.isEmpty() && stagedRemove.isEmpty()) {
            System.out.println("No changes added to the commit.");
        } else {
            //make commit object
            Date now = new Date();
            SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            String commitTime = timeFormat.format(now);
            Commit newCommit = new Commit(message, commitTime, head, branchCommit);

            //set HEAD
            head = newCommit;

            //set branch
            branches.remove(currentBranch);
            branches.put(currentBranch, newCommit);

            //save to commit.txt
            commitMap = Utils.readObject(COMMITMAP, HashMap.class);
            commitMap.put(newCommit.getID(), newCommit);

            //clear staging area
            stagedAdd.clear();
            stagedRemove.clear();
            Utils.writeContents(ADDMAP, Utils.serialize(stagedAdd));
            Utils.writeContents(REMOVEMAP, Utils.serialize(stagedRemove));
            Utils.writeContents(COMMITMAP, Utils.serialize(commitMap));
            //save tree
            tree.setStagedAdd(stagedAdd);
            tree.setStagedRemove(stagedRemove);
            tree.setCommitMap(commitMap);
            tree.setHEAD(head);
            tree.setBranches(branches);
            tree.setCurrentBranch(currentBranch);
            Utils.writeContents(MAPS, Utils.serialize(tree));
        }
    }

    public static void checkoutFile(String fileName) throws IOException {
        Tree tree = readObject(MAPS, Tree.class);
        Commit head = tree.getHEAD();

        if (head.getCommitted().containsKey(fileName)) {
            //delete if there is the file in cwd
            boolean checkExist = Utils.join(CWD, fileName).exists();
            if (checkExist) {
                restrictedDelete(Utils.join(CWD, fileName));
            }
            File newFile = Utils.join(CWD, fileName);

            byte[] content = head.getCommitted().get(fileName);
            Utils.writeContents(newFile, content);
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    public static void checkoutCommit(String commitID, String fileName) {
        Tree tree = readObject(MAPS, Tree.class);
        HashMap<String, Commit> commitMap = tree.getCommitMap();
        commitMap = tree.getCommitMap();

        Boolean check = false;
        String fullCommitID = "";
        for (String fullID : commitMap.keySet()) {
            if (fullID.contains(commitID)) {
                check = true;
                fullCommitID = fullID;
            }
        }
        if (!check) {
            System.out.println("No commit with that id exists.");
        } else if (!commitMap.get(fullCommitID).getCommitted().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
        } else {
            File checkoutFile = Utils.join(CWD, fileName);
            byte[] content1 = commitMap.get(fullCommitID).getCommitted().get(fileName);
            Utils.writeContents(checkoutFile, content1);
        }
    }
    public static void checkoutBranch(String branchName) {
        Tree tree = readObject(MAPS, Tree.class);
        HashMap<String, byte[]> stagedAdd = tree.getStagedAdd();
        HashMap<String, byte[]> stagedRemove = tree.getStagedRemove();
        HashMap<String, Commit> commitMap = tree.getCommitMap();
        Commit head = tree.getHEAD();
        HashMap<String, Commit> branches = tree.getBranches();
        String currentBranch = tree.getCurrentBranch();

        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        } else if (currentBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        } else {
            String[] listofFiles = CWD.list();
            //check untracked existence
            for (String checkOverWrite : listofFiles) {
                if (branches.get(branchName).getCommitted().containsKey(checkOverWrite)
                        && !head.getCommitted().containsKey(checkOverWrite)) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it, or add and commit it first.");
                    return;
                }
            }

            //clear staging area
            stagedAdd.clear();
            stagedRemove.clear();

            Commit commitOfGivenBranch = branches.get(branchName);
            for (File a : CWD.listFiles()) {
                a.delete();
            }
            for (String file : commitOfGivenBranch.getCommitted().keySet()) {
                File checkoutFile = Utils.join(CWD, file);
                byte[] content = commitOfGivenBranch.getCommitted().get(file);
                Utils.writeContents(checkoutFile, content);
            }
            //set given branch as HEAD
            head = commitOfGivenBranch;
            currentBranch = branchName;
            //save
            tree.setStagedAdd(stagedAdd);
            tree.setStagedRemove(stagedRemove);
            tree.setCommitMap(commitMap);
            tree.setHEAD(head);
            tree.setBranches(branches);
            tree.setCurrentBranch(currentBranch);

            Utils.writeContents(MAPS, Utils.serialize(tree));
        }
    }


    public static void log() {
        Tree tree = readObject(MAPS, Tree.class);
        Commit head = tree.getHEAD();
        Commit pointer = head;
        while (pointer != null) {
            pointer.log();
            pointer = pointer.getParent();
        }

    }

    public static void globalLog() {
        Tree tree = Utils.readObject(MAPS, Tree.class);
        HashMap<String, Commit> commitMap = tree.getCommitMap();
        commitMap = Utils.readObject(COMMITMAP, HashMap.class);
        for (Commit a : commitMap.values()) {
            a.log();
        }
    }

    public static void branch(String newBranchName) {

        Tree tree = readObject(MAPS, Tree.class);
        HashMap<String, Commit> branches = tree.getBranches();
        Commit head = tree.getHEAD();
        if (branches.keySet().contains(newBranchName)) {
            System.out.println("A branch with that name already exists.");
        } else {
            Commit currHEAD = head;
            branches.put(newBranchName, currHEAD);
        }
        tree.setBranches(branches);
        Utils.writeContents(MAPS, Utils.serialize(tree));
    }

    public static void rmBranch(String branchName) {
        Tree tree = Utils.readObject(MAPS, Tree.class);
        HashMap<String, Commit> branches = tree.getBranches();
        String currentBranch = tree.getCurrentBranch();
        if (currentBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else if (branches.containsKey(branchName)) {
            branches.remove(branchName);
            tree.setBranches(branches);
            Utils.writeContents(MAPS, Utils.serialize(tree));
        } else {
            System.out.println("A branch with that name does not exist.");
        }
    }


    public void status() {
        Tree tree = readObject(MAPS, Tree.class);
        HashMap<String, Commit> branches = tree.getBranches();
        String currentBranch = tree.getCurrentBranch();
        HashMap<String, byte[]> stagedAdd = tree.getStagedAdd();
        HashMap<String, byte[]> stagedRemove = tree.getStagedRemove();

        System.out.println("=== Branches ===");
        if (branches.size() == 1) {
            System.out.println("*main");
            System.out.println();
        } else {
            String main = "";
            String other = "";
            for (String branchName : branches.keySet()) {
                if (branchName.equals("main")) {
                    if (currentBranch.equals(branchName)) {
                        main = "*main";
                    } else {
                        main = "main";
                    }
                } else {
                    if (currentBranch == branchName) {
                        other = "*" + branchName;
                    } else {
                        other = branchName;
                    }
                }
            }
            System.out.println(main);
            System.out.println(other);
            System.out.println();
        }

        //staged files
        System.out.println("=== Staged Files ===");
        List<String> stagedInOrder = new ArrayList<String>();
        for (String toAdd : stagedAdd.keySet()) {
            stagedInOrder.add(toAdd);
        }
        Collections.sort(stagedInOrder);
        for (String stagedAddString : stagedInOrder) {
            System.out.println(stagedAddString);
        }
        System.out.println();

        //removed files
        System.out.println("=== Removed Files ===");
        List<String> removedInOrder = new ArrayList<String>();
        for (String toRemove : stagedRemove.keySet()) {
            removedInOrder.add(toRemove);
        }
        Collections.sort(removedInOrder);
        for (String stagedRemoveString : removedInOrder) {
            System.out.println(stagedRemoveString);
        }
        System.out.println();

        //modifications not staged
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        //untracked files
        System.out.println("=== Untracked Files ===");
    }

    public void find(String cmsg) {

        Tree tree = Utils.readObject(MAPS, Tree.class);
        HashMap<String, Commit> commitMap = tree.getCommitMap();
        commitMap = Utils.readObject(COMMITMAP, HashMap.class);
        Boolean checkExist = false;
        for (Commit a : commitMap.values()) {
            if (a.getMessage().equals(cmsg)) {
                System.out.println(a.getID());
                checkExist = true;
            }
        }
        if (!checkExist) {
            System.out.println("Found no commit with that message.");
        }
    }

    private boolean mergeCheck(String branchName) {
        Tree tree = Utils.readObject(MAPS, Tree.class);
        HashMap<String, byte[]> stagedAdd = tree.getStagedAdd();
        HashMap<String, byte[]> stagedRemove = tree.getStagedRemove();
        HashMap<String, Commit> commitMap = tree.getCommitMap();
        Commit head = tree.getHEAD();
        HashMap<String, Commit> branches = tree.getBranches();
        String currentBranch = tree.getCurrentBranch();
        //check errors and exceptions
        if (!stagedAdd.isEmpty() || !stagedRemove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return false;
        } else if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return false;
        } else if (branches.get(branchName).equals(head)) {
            System.out.println("Cannot merge a branch with itself.");
            return false;
        }

        //check untracked existence
        String[] listOfFiles = CWD.list();
        for (String checkOverWrite : listOfFiles) {
            if (branches.get(branchName).getCommitted().containsKey(checkOverWrite)
                    && !head.getCommitted().containsKey(checkOverWrite)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                return false;
            }
        }
        return true;
    }

    public Commit findSplitPoint(String branchName) {
        Tree tree = Utils.readObject(MAPS, Tree.class);
        Commit head = tree.getHEAD();
        HashMap<String, Commit> branches = tree.getBranches();

        //find split point
        Commit headPtr = head;
        Commit branchPtr = branches.get(branchName);
        Commit splitPoint = null;
        HashSet<String> current = new HashSet<>();
        Commit branchParentExistPtr = null;
        Commit ptr2 = null;

        while (headPtr != null) {
            if (headPtr.getBranchParent() != null) {
                branchParentExistPtr = headPtr.getBranchParent();
            }
            current.add(headPtr.getID());
            headPtr = headPtr.getParent();
        }
        while (branchParentExistPtr != null) {
            current.add(branchParentExistPtr.getID());
            if (branchParentExistPtr.getBranchParent() != null) {
                ptr2 = branchParentExistPtr.getBranchParent();
            }
            branchParentExistPtr = branchParentExistPtr.getParent();
        }
        while (ptr2 != null) {
            current.add(ptr2.getID());
            ptr2 = ptr2.getParent();
        }
        while (branchPtr != null) {
            if (current.contains(branchPtr.getID())) {
                splitPoint = branchPtr;
                break;
            }
            branchPtr = branchPtr.getParent();
        }
        //check ancestor
        if (current.contains(branches.get(branchName).getID())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return null;
        }
        //check fast-forward
        if (splitPoint.getID().equals(head.getID())) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(branchName);
            return null;
        }
        return splitPoint;
    }

    public void merge(String branchName) {
        Tree tree = Utils.readObject(MAPS, Tree.class);
        HashMap<String, Commit> branches = tree.getBranches();
        String currentBranch = tree.getCurrentBranch();
        if (!mergeCheck(branchName)) {
            return;
        }
        Commit splitPoint = findSplitPoint(branchName);
        if (splitPoint == null) {
            return;
        }
        //merge
        HashMap<String, byte[]> headFiles = tree.getHEAD().getCommitted();
        HashMap<String, byte[]> branchFiles = branches.get(branchName).getCommitted();
        HashMap<String, byte[]> splitFiles = splitPoint.getCommitted();
        boolean checkConflict = false;
        for (String file : headFiles.keySet()) {
            byte[] headContent = headFiles.get(file);
            byte[] branchContent = branchFiles.get(file);
            byte[] splitContent = splitFiles.get(file);
            if (splitFiles.containsKey(file) && branchFiles.containsKey(file)) {
                if (Arrays.equals(headContent, splitContent)
                        && !Arrays.equals(branchContent, splitContent)) {
                    checkoutCommit(branches.get(branchName).getID(), file);
                    add(file);
                }
                if (!Arrays.equals(headContent, branchContent)
                        && !Arrays.equals(splitContent, headContent)
                        && !Arrays.equals(splitContent, branchContent)) {
                    File newFile = Utils.join(CWD, file);
                    byte[] newContent =
                            addContent("<<<<<<< HEAD\n".getBytes(StandardCharsets.UTF_8),
                            headContent);
                    newContent = addContent(newContent,
                            "=======\n".getBytes(StandardCharsets.UTF_8));
                    newContent = addContent(newContent, branchContent);
                    newContent = addContent(newContent,
                            ">>>>>>>\n".getBytes(StandardCharsets.UTF_8));
                    Utils.writeContents(newFile, newContent);
                    checkConflict = true;
                }
            } else if (!branchFiles.containsKey(file) && splitFiles.containsKey(file)) {
                if (!Arrays.equals(splitContent, headContent)) {
                    File newFile = Utils.join(CWD, file);
                    byte[] newContent =
                            addContent("<<<<<<< HEAD\n".getBytes(StandardCharsets.UTF_8),
                                    headContent);
                    newContent = addContent(newContent,
                            "=======\n".getBytes(StandardCharsets.UTF_8));
                    newContent = addContent(newContent,
                            ">>>>>>>\n".getBytes(StandardCharsets.UTF_8));
                    Utils.writeContents(newFile, newContent);
                    checkConflict = true;
                }
            }
        }
        for (String file : branches.get(branchName).getCommitted().keySet()) {
            if (!splitFiles.containsKey(file)) {
                checkoutCommit(branches.get(branchName).getID(), file);
                add(file);
            }
        }
        for (String file : splitPoint.getCommitted().keySet()) {
            if (headFiles.containsKey(file) && !branchFiles.containsKey(file)) {
                if (Arrays.equals(splitPoint.getCommitted().get(file), headFiles.get(file))) {
                    rm(file);
                }
            }
        }
        if (checkConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        mergeCommit("Merged " + branchName + " into "
                + currentBranch + ".", branches.get(branchName));
    }

    private byte[] addContent(byte[] first, byte[] second) {
        byte[] toReturn = new byte[first.length + second.length];
        System.arraycopy(first, 0, toReturn, 0, first.length);
        System.arraycopy(second, 0, toReturn, first.length, second.length);
        return toReturn;
    }

    public static void reset(String commitID) {
        Tree tree = Utils.readObject(MAPS, Tree.class);
        HashMap<String, Commit> commitMap = tree.getCommitMap();
        Commit head = tree.getHEAD();
        commitMap = Utils.readObject(COMMITMAP, HashMap.class);
        HashMap<String, byte[]> stagedAdd = tree.getStagedAdd();
        HashMap<String, byte[]> stagedRemove = tree.getStagedRemove();
        String currentBranch = tree.getCurrentBranch();
        HashMap<String, Commit> branches = tree.getBranches();

        //check if commit exists, or untracked file exists
        if (!commitMap.containsKey(commitID)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        String[] listOfFiles = CWD.list();
        //check untracked existence
        for (String checkOverWrite : listOfFiles) {
            if (commitMap.get(commitID).getCommitted().containsKey(checkOverWrite)
                    && !head.getCommitted().containsKey(checkOverWrite)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                return;
            }
        }

        //delete
        for (String fileName : listOfFiles) {
            if (!commitMap.get(commitID).getCommitted().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }

        //add
        for (String fileName : commitMap.get(commitID).getCommitted().keySet()) {
            File newly = Utils.join(GITLET_DIR, fileName);
            Utils.writeContents(newly, commitMap.get(commitID).getCommitted().get(fileName));
        }

        head = commitMap.get(commitID);
        String currBranch = currentBranch;
        branches.remove(currBranch);
        branches.put(currBranch, head);

        stagedAdd.clear();
        stagedRemove.clear();
        Utils.writeContents(ADDMAP, Utils.serialize(stagedAdd));
        Utils.writeContents(REMOVEMAP, Utils.serialize(stagedRemove));

        tree.setStagedAdd(stagedAdd);
        tree.setStagedRemove(stagedRemove);
        tree.setHEAD(head);
        tree.setBranches(branches);
        tree.setCurrentBranch(currentBranch);
        tree.setCommitMap(commitMap);
        Utils.writeContents(MAPS, Utils.serialize(tree));
    }

}




