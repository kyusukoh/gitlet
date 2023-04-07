package gitlet;

import java.io.Serializable;
import java.util.HashMap;

public class Tree implements Serializable {

    private HashMap<String, byte[]> stagedAdd = new HashMap<>();
    //serialized, readcontentasString 한거를 넣어놓는 거: 모든 add들을 넣어보자
    public HashMap<String, byte[]> stagedAdd() {
        return stagedAdd;
    }
    public HashMap<String, byte[]> getStagedAdd() {
        return stagedAdd;
    }
    public void setStagedAdd(HashMap<String, byte[]> stagedAdd) {
        this.stagedAdd = stagedAdd;
    }
    private HashMap<String, byte[]> filesMap = new HashMap<>();
    public HashMap<String, byte[]> getFilesMap() {
        return filesMap;
    }
    public void setFilesMap(HashMap<String, byte[]> filesMap) {
        this.filesMap = filesMap;
    }
    private HashMap<String, byte[]> stagedRemove = new HashMap<>();
    public HashMap<String, byte[]> getStagedRemove() {
        return stagedRemove;
    }
    public void setStagedRemove(HashMap<String, byte[]> stagedRemove) {
        this.stagedRemove = stagedRemove;
    }
    private HashMap<String, Commit> commitMap = new HashMap<>();
    public HashMap<String, Commit> getCommitMap() {
        return commitMap;
    }
    public void setCommitMap(HashMap<String, Commit> commitMap) {
        this.commitMap = commitMap;
    }
    private HashMap<String, Commit> branches = new HashMap<String, Commit>();
    public HashMap<String, Commit> getBranches() {
        return branches;
    }
    public void setBranches(HashMap<String, Commit> branches) {
        this.branches = branches;
    }
    private String currentBranch;
    public String getCurrentBranch() {
        return currentBranch;
    }
    public void setCurrentBranch(String currentBranch) {
        this.currentBranch = currentBranch;
    }
    private Commit head = new Commit("dummy", null, null);
    public Commit getHEAD() {
        return head;
    }
    public void setHEAD(Commit toBeHead) {
        this.head = toBeHead;
    }
}
