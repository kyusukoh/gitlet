package gitlet;

import java.util.*;
import java.io.Serializable;


/** Represents a gitlet commit object.
 *  @author Kyusuk Oh
 */
public class Commit implements Serializable {
    private String message;
    private String ID;
    private String time;
    private Commit parent;
    private Commit branchParent;
    private String mergeID;
    private boolean isMerge;
    private final HashMap<String, byte[]> committed = new HashMap<>();
    public Commit(String msg, String time, Commit parent) {
        this.message = msg;
        this.parent = parent;
        this.branchParent = null;
        this.mergeID = "";
        this.isMerge = false;

        if (parent != null) {
            committed.putAll(parent.getCommitted());
            this.time = time;
        } else {
            this.time = "Wed Dec 31 16:00:00 1969 -0800";
        }

        //Update files to add then empty add
        HashMap<String, byte[]> toAdd = Utils.readObject(Repository.ADDMAP, HashMap.class);
        committed.putAll(toAdd);

        //save to filesMap (inside if to avoid error during init)
        if (parent != null) {
            Tree tree = Utils.readObject(Repository.MAPS, Tree.class);
            HashMap<String, byte[]> filesMap = tree.getFilesMap();
            filesMap.putAll(toAdd);
            tree.setFilesMap(filesMap);
            Utils.writeContents(Repository.MAPS, Utils.serialize(tree));
        }
        //clear toAdd
        toAdd.clear();
        Utils.writeContents(Repository.ADDMAP, Utils.serialize(toAdd));

        //Update files to remove then empty remove
        HashMap<String, byte[]> toRemove = Utils.readObject(Repository.REMOVEMAP, HashMap.class);
        committed.keySet().removeAll(toRemove.keySet());
        toRemove.clear();

        if (committed.isEmpty()) {
            this.ID = Utils.sha1(message);
        } else {
            this.ID = Utils.sha1(message, committed.keySet().toString(),
                    committed.values().toString());
        }
        Utils.writeContents(Repository.REMOVEMAP, Utils.serialize((toRemove)));
    }

    public Commit(String msg, String time, Commit parent, Commit branchParent) {
        this.message = msg;
        this.time = time;
        this.parent = parent;
        this.branchParent = branchParent;
        this.isMerge = true;
        this.mergeID = parent.getID().substring(0, 7) + " "
                + branchParent.getID().substring(0, 7);
        this.committed.putAll(parent.getCommitted());

        //Update files to add then empty add
        HashMap<String, byte[]> toAdd = Utils.readObject(Repository.ADDMAP, HashMap.class);
        committed.putAll(toAdd);

        //save to filesMap (inside if to avoid error during init)
        if (parent != null) {
            Tree tree = Utils.readObject(Repository.MAPS, Tree.class);
            HashMap<String, byte[]> filesMap = tree.getFilesMap();
            filesMap.putAll(toAdd);
            tree.setFilesMap(filesMap);
            Utils.writeContents(Repository.MAPS, Utils.serialize(tree));
        }
        //clear toAdd
        toAdd.clear();
        Utils.writeContents(Repository.ADDMAP, Utils.serialize(toAdd));

        //Update files to remove then empty remove
        HashMap<String, byte[]> toRemove = Utils.readObject(Repository.REMOVEMAP, HashMap.class);
        committed.keySet().removeAll(toRemove.keySet());
        toRemove.clear();

        if (committed.isEmpty()) {
            this.ID = Utils.sha1(message);
        } else {
            this.ID = Utils.sha1(message, committed.keySet().toString(),
                    committed.values().toString());
        }
        Utils.writeContents(Repository.REMOVEMAP, Utils.serialize((toRemove)));
    }

    public String getID() {
        return this.ID;
    }

    public String getMessage() {
        return this.message;
    }

    public String getTime() {
        return this.time;
    }

    public HashMap<String, byte[]> getCommitted() {
        return this.committed;
    }

    public Commit getParent() {
        return this.parent;
    }
    public Commit getBranchParent() {
        return this.branchParent;
    }
    public String getMergeID() {
        return this.mergeID;
    }
    public void log() {
        if (isMerge) {
            System.out.println("===");
            System.out.println("commit " + this.getID());
            System.out.println("Merge: " + mergeID);
            System.out.println("Date: " + this.getTime());
            System.out.println(this.getMessage());
            System.out.println();
            return;
        }

        System.out.println("===");
        System.out.println("commit " + this.getID());
        System.out.println("Date: " + this.getTime());
        System.out.println(this.getMessage());
        System.out.println();
    }

    public String makeLog() {
        return "===\n" + "commit " + this.getID() + "\n"
                + "Date: " + this.getTime() + "\n"
                + this.getMessage() + "\n" + "\n";
    }
}

