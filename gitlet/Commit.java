package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
/** Commit class for Gitlet.
 *  @author Yu Jia Xu
 */
public class Commit implements Serializable {

    /**
     * working directory.
     */
    static final File CWD = new File(".");
    /** Main metadata folder. */
    private static File mainfolder  = Utils.join(CWD, ".gitlet");

    /**
     * commit folder.
     */
    private static File commitfolder = Utils.join(mainfolder, "commit_folder");

    /**
     * date.
     */
    private Date date;

    /**
     * commit message.
     */
    private String message;

    /**
     * commit folder.
     */
    private File CF;

    /**
     * main parent.
     */
    private String parent;

    /**
     *
     * @return parent
     */
    public String getP() {
        return parent;
    }

    /**
     *
     * @param p new parent
     */
    public void setP(String p) {
        parent = p;
    }

    /**
     * subparent.
     */
    private String subparent;
    /**
     * name of files.
     */
    private HashMap<String, String> files;

    /**
     *
     * @return file set
     */
    public HashMap<String, String> getFiles() {
        return files;
    }

    /**
     *
     * @param key file to remove
     */
    public void removefile(String key) {
        files.remove(key);
    }

    /**
     * ancestor set of this commit.
     */
    private HashMap<String, Integer> ancestor;

    /**
     * ancestor sets for given id.
     */
    private ArrayList<String> inancestor;

    /**
     *
     * @param d date
     * @param m message
     * @param cf commit folder
     */
    Commit(Date d, String m, File cf) {
        files = new HashMap<>();
        CF = cf;
        this.date = d;
        this.message = m;
    }

    /**
     *
     * @return main parent
     */
    public String getparent() {
        return parent;
    }

    /**
     *
     * @param name name in commit folder
     * @return commit with that id
     */
    public static Commit fromFile(String name) {
        File commitFile = Utils.join(commitfolder, name);
        return Utils.readObject(commitFile, Commit.class);
    }

    /**
     *
     * @param sha1commit commit of subparent
     */
    public void setSubparent(String sha1commit) {
        subparent = sha1commit;
    }

    /**
     *
     * @return sub parent
     */
    public String getSubparent() {
        return subparent;
    }


    /**
     *
     * @param name name of file
     * @param con content of file
     */
    public void addfiles(String name, String con) {
        files.put(name, con);
    }

    /**
     *
     * @return date
     */
    public Date getDate() {
        return date;
    }

    /**
     *
     * @return commit message
     */
    public String getMessage() {
        return message;
    }

    /**
     *
     * @param d time
     * @param m message
     * @return chlid of this commit
     */
    public Commit createChild(Date d, String m) {
        Commit child = new Commit(d, m, commitfolder);
        for (String filename: files.keySet()) {
            child.addfiles(filename, files.get(filename));
        }
        return child;
    }

    /**
     * save current commit.
     */
    public void saveCommit() {
        Utils.writeObject(Utils.join(CF, sha1()), this);
    }

    /**
     *
     * @return sha1 code of this class
     */
    public String sha1() {
        return Utils.sha1(date.toString(), message, parent, files.toString());
    }

    /**
     * setup the ancestor set.
     */
    public void setupancestor() {
        ancestor = new HashMap<>();
        inancestor = new ArrayList<>();
        ancestor.put(sha1(), 0);
        doactualsetup(parent, subparent, 1);
    }

    /**
     *
     * @param firstparent main parent
     * @param secondparent sub parent
     * @param step step counting
     */
    public void doactualsetup(String firstparent,
                              String secondparent, Integer step) {
        if (!firstparent.equals("null")) {
            if (ancestor.containsKey(firstparent)) {
                if (ancestor.get(firstparent) > step) {
                    ancestor.put(firstparent, step);
                }
            } else {
                ancestor.put(firstparent, step);
            }
            Commit firstcommit = fromFile(firstparent);
            String firstsub = firstcommit.getparent();
            String secondsub = firstcommit.getSubparent();
            doactualsetup(firstsub, secondsub, step + 1);
        }
        if (secondparent != null) {
            if (ancestor.containsKey(secondparent)) {
                if (ancestor.get(secondparent) > step) {
                    ancestor.put(secondparent, step);
                }
            } else {
                ancestor.put(secondparent, step);
            }
            Commit secondcommit = fromFile(secondparent);
            String firstsub = secondcommit.getparent();
            String secondsub = secondcommit.getSubparent();
            doactualsetup(firstsub, secondsub, step + 1);
        }
    }

    /**
     *
     * @param branchid given id
     */
    public void setupinancestor(String branchid) {
        inancestor.add(branchid);
        Commit branchcommit = fromFile(branchid);
        String firstparent = branchcommit.getparent();
        String secondparent = branchcommit.getSubparent();
        doinancestor(firstparent, secondparent);
    }

    /**
     *
     * @param firstparent main parent
     * @param secondparent sub parent
     */
    public void doinancestor(String firstparent, String secondparent) {
        if (!firstparent.equals("null")) {
            if (!inancestor.contains(firstparent)) {
                inancestor.add(firstparent);
            }
            Commit firstcommit = fromFile(firstparent);
            String firstsub = firstcommit.getparent();
            String secondsub = firstcommit.getSubparent();
            doinancestor(firstsub, secondsub);
        }
        if (secondparent != null) {
            if (!inancestor.contains(secondparent)) {
                inancestor.add(secondparent);
            }
            Commit secondcommit = fromFile(secondparent);
            String firstsub = secondcommit.getparent();
            String secondsub = secondcommit.getSubparent();
            doinancestor(firstsub, secondsub);
        }
    }

    /**
     * a very large number.
     */
    static final int LARGE = 99999;

    /**
     *
     * @return common ancestor
     */
    public String getcommonan() {
        String ans = inancestor.get(0);
        Integer min = LARGE;
        for (String ele: inancestor) {
            Integer temp = ancestor.get(ele);
            if (temp != null && temp < min) {
                min = temp;
                ans = ele;
            }
        }
        return ans;
    }

    /**
     * for cleaning up the sets.
     */
    public void cleanupmergeset() {
        ancestor = null;
        inancestor = null;
    }

    /**
     *
     * @param branchid given branchid
     * @param check nothing just for checking
     * @return the string of split point
     */
    public String findleastans(String branchid, boolean check) {
        setupancestor();
        setupinancestor(branchid);
        String result = getcommonan();
        if (check) {
            System.out.println(ancestor.toString());
            System.out.println(inancestor.toString());
            System.out.println(result);
        }

        cleanupmergeset();
        return result;
    }



}
