package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;


/** this state is for tracking the newest commit file for Gitlet.
 *  @author Yu Jia Xu
 */
public class CState implements Serializable {

    /**
     * working directory.
     */
    static final File CWD = new File(".");
    /** Main metadata folder. */
    private static File mainfolder  = Utils.join(CWD, ".gitlet");

    /**
     * state folder.
     */
    private static File statefolder = Utils.join(mainfolder, "state_folder");

    /**
     * sha1 code of current commit.
     */
    private String commitsha1;

    /**
     * all branches including with the current branches.
     */
    private HashMap<String, String> branches;

    /**
     * the files that should not be included in the next branches.
     */
    private HashMap<String, String> rmfiles;

    /**
     *
     * @return file set
     */
    public HashMap<String, String> getRmfiles() {
        return rmfiles;
    }

    /**
     *
     * @param key file to remove
     */
    public void removermfile(String key) {
        rmfiles.remove(key);
    }

    /**
     * current branch.
     */
    private String currentbranch;

    /**
     *
     * @return current branch
     */
    public String gcur() {
        return currentbranch;
    }

    /**
     * construction.
     */
    CState() {
        branches = new HashMap<>();
        rmfiles = new HashMap<>();
    }

    /**
     *
     * @param name name of file
     * @param content content of this file
     */
    public void savermfiles(String name, String content) {
        rmfiles.put(name, content);
    }

    /**
     * clear rm sets.
     */
    public void cleanuprmfiles() {
        rmfiles.clear();
    }

    /**
     *
     * @param name branch
     */
    public void removebranch(String name) {
        branches.remove(name);
    }

    /**
     *
     * @return current branch
     */
    public String getCurrentbranch() {
        return currentbranch;
    }

    /**
     *
     * @return this state class
     */
    public static CState fromFile() {
        File commitFile = Utils.join(statefolder, "state");
        return Utils.readObject(commitFile, CState.class);
    }

    /**
     * save this state.
     */
    public void saveState() {
        Utils.writeObject(Utils.join(statefolder, "state"), this);
    }

    /**
     *
     * @return branch sha1
     */
    public String getCommitsha1() {
        return commitsha1;
    }

    /**
     *
     * @param cs1 sha1
     */
    public void setCommitsha1(String cs1) {
        commitsha1 = cs1;
    }

    /**
     *
     * @param branchname name of branch
     */
    public void setCurrentbranch(String branchname) {
        currentbranch = branchname;
    }

    /**
     *
     * @param branchname name of branch
     */
    public void putbranch(String branchname) {
        branches.put(branchname, getCommitsha1());
    }

    /**
     *
     * @param branchname name of branch
     * @param branchcontent id
     */
    public void putbranch(String branchname, String branchcontent) {
        branches.put(branchname, branchcontent);
    }

    /**
     *
     * @param branchname name of branch
     * @return id branch
     */
    public String getbranchwithname(String branchname) {
        return branches.get(branchname);
    }

    /**
     *
     * @return branches
     */
    public HashMap<String, String> getbranches() {
        return branches;
    }



}
