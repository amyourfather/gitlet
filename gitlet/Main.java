package gitlet;


import java.io.File;
import java.util.Date;
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import java.text.SimpleDateFormat;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Yu Jia Xu
 */
public class Main {
    /** Working Directory folder. */
    static final File CWD = new File(".");
    /** Main metadata folder. */
    private static File mainfolder  = Utils.join(CWD, ".gitlet");
    /** Stage file folder. */
    private static File stagefolder = Utils.join(mainfolder, "stage_folder");
    /** Commit class folder. */
    private static File commitfolder = Utils.join(mainfolder, "commit_folder");
    /** Current State file folder. */
    private static File statefolder = Utils.join(mainfolder, "state_folder");
    /** Blob Content folder. */
    private static File blobfolder = Utils.join(mainfolder, "blob_folder");
    /** Remove files folder. */
    private static File rmfolder = Utils.join(mainfolder, "rm_folder");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            exitWithError("Must have at least one argument");
        }
        mainerror(args);
        switch (args[0]) {
        case "init":
            doinit(); break;
        case "commit":
            Date date = new Date();
            docommit(args, date, false, null, null); break;
        case "add":
            doadd(args); break;
        case "log":
            dolog(args); break;
        case "checkout":
            docheckout(args); break;
        case "rm":
            dorm(args); break;
        case "global-log":
            dogloballog(args); break;
        case "branch":
            dobranch(args); break;
        case "find":
            dofind(args); break;
        case "status":
            dostatus(args); break;
        case "rm-branch":
            dormbranch(args); break;
        case "reset":
            doreset(args, true); break;
        case "merge":
            domerge(args); break;
        default:
            exitWithError("No command with that name exists.");
        }
        System.exit(0);
    }

    /**
     *
     * @param args arguments
     */
    public static void mainerror(String[] args) {
        switch (args[0]) {
        case "init":
            if (args.length != 1) {
                exitWithError("Incorrect operands.");
            }
            break;
        case "commit":
            if (args.length > 2) {
                exitWithError("Incorrect operands.");
            } else if (args[1].equals("")) {
                exitWithError("Please enter a commit message.");
            }
            break;
        case "add":
        case "merge":
        case "reset":
        case "rm-branch":
        case "find":
        case "branch":
        case "rm":
            if (args.length > 2) {
                exitWithError("Incorrect operands.");
            }
            break;
        case "log":
        case "status":
        case "global-log":
            if (args.length > 1) {
                exitWithError("Incorrect operands.");
            }
            break;
        case "checkout":
            if (args.length > 4 || args.length < 2) {
                exitWithError("Incorrect operands.");
            } else if (args.length == 4 && !args[2].equals("--")) {
                exitWithError("Incorrect operands.");
            } else if (args.length == 3 && !args[1].equals("--")) {
                exitWithError("Incorrect operands.");
            }
            break;
        default:
            exitWithError("No command with that name exists.");
        }
    }

    /**
     *
     * @param message error message
     */
    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

    /**
     * do init.
     */
    public static void doinit() {
        if (!mainfolder.exists()) {
            mainfolder.mkdirs();
            stagefolder.mkdirs();
            commitfolder.mkdirs();
            statefolder.mkdirs();
            blobfolder.mkdirs();
            rmfolder.mkdirs();
            Date inidate = new Date(0);
            String inimessage = "initial commit";
            String[] ini = {"commit", inimessage};
            Commit cm = docommit(ini, inidate, true, null, null);
            CState state = new CState();
            cm.setP("null");
            cm.saveCommit();
            state.setCommitsha1(cm.sha1());
            state.setCurrentbranch("master");
            state.putbranch("master");
            state.saveState();
        } else {
            exitWithError("A Gitlet version-control"
                    +
                    " system already exists in the current directory.");
        }
    }

    /**
     *
     * @param args arguments
     * @param date time now
     * @param first init or not
     * @param branchid given branch
     * @param curid current branch
     * @return new commit
     */
    public static Commit docommit(String[] args,
                                  Date date, boolean first,
                                  String branchid, String curid) {
        File stateFile = Utils.join(statefolder, "state");
        if (first) {
            return new Commit(date, args[1], commitfolder);
        }
        CState oldstate = Utils.readObject(stateFile, CState.class);
        Commit old = Commit.fromFile(oldstate.getCommitsha1());
        Commit cm = old.createChild(date, args[1]);
        List<String> dir = Utils.plainFilenamesIn(stagefolder);
        cm.setP(old.sha1());
        if (oldstate.getRmfiles().isEmpty() && dir.size() == 0) {
            exitWithError("No changes added to the commit.");
        }
        for (String key: oldstate.getRmfiles().keySet()) {
            cm.removefile(key);
            File rmFile = Utils.join(rmfolder, key);
            rmFile.delete();
        }
        for (String name: dir) {
            File temp = Utils.join(stagefolder, name);
            String tempcontent = Utils.readContentsAsString(temp);
            String blobsha1 = Utils.sha1(name, tempcontent);
            File blobFile = Utils.join(blobfolder, blobsha1);
            Utils.writeContents(blobFile, tempcontent);
            cm.addfiles(name, blobsha1);
            temp.delete();
        }
        if (branchid != null && curid != null) {
            cm.setP(curid);
            cm.setSubparent(branchid);
        } else {
            cm.setSubparent(old.getSubparent());
        }
        cm.saveCommit();
        CState state = CState.fromFile();
        state.cleanuprmfiles();
        List<String> dir2 = Utils.plainFilenamesIn(rmfolder);
        if (dir2 != null) {
            for (String ele: dir2) {
                File rmFile = Utils.join(rmfolder, ele);
                rmFile.delete();
            }
        }
        state.setCommitsha1(cm.sha1());
        state.putbranch(state.getCurrentbranch());
        state.saveState();
        return cm;
    }

    /**
     *
     * @param args arguments
     */
    public static void doadd(String[] args) {
        boolean check = true;
        CState currentstate = CState.fromFile();
        String headsha1 = currentstate.getCommitsha1();
        File stageFile = Utils.join(stagefolder, args[1]);
        File originFile = Utils.join(CWD, args[1]);
        if (!originFile.exists()) {
            exitWithError("File does not exist.");
        }
        String content = Utils.readContentsAsString(originFile);
        if (!headsha1.equals("null")) {
            Commit headcommit = Commit.fromFile(headsha1);
            if (headcommit.getFiles().containsKey(args[1])) {
                File headfile = Utils.join(blobfolder,
                        headcommit.getFiles().get(args[1]));
                String oldcontent = Utils.readContentsAsString(headfile);
                if (oldcontent.equals(content)) {
                    check = false;
                    if (stageFile.exists()) {
                        String stagecontent =
                                Utils.readContentsAsString(stageFile);
                        if (stagecontent.equals(content)) {
                            stageFile.delete();
                        }
                    }
                }
            }
        }
        if (currentstate.getRmfiles().containsKey(args[1])) {
            File thatrmfile = Utils.join(rmfolder, args[1]);
            String rmcontent = Utils.readContentsAsString(thatrmfile);
            if (rmcontent.equals(content)) {
                currentstate.removermfile(args[1]);
                thatrmfile.delete();
                currentstate.saveState();
            }
        }
        if (check) {
            Utils.writeContents(stageFile, content);
        }
        currentstate.saveState();
    }

    /**
     *
     * @param args arguments
     */
    public static void dolog(String[] args) {
        SimpleDateFormat format = new
                SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
        CState currentstate = CState.fromFile();
        String csha1 = currentstate.getCommitsha1();
        Commit newestcommit = Commit.fromFile(csha1);
        String formattime;
        while (!newestcommit.getP().equals("null")) {
            System.out.println("===");
            System.out.println("commit " + newestcommit.sha1());
            formattime = format.format(newestcommit.getDate());
            System.out.println("Date: " + formattime);
            System.out.println(newestcommit.getMessage());
            System.out.println();
            newestcommit = Commit.fromFile(newestcommit.getP());
        }

        System.out.println("===");
        System.out.println("commit " + newestcommit.sha1());
        formattime = format.format(newestcommit.getDate());
        System.out.println("Date: " + formattime);
        System.out.println(newestcommit.getMessage());
        System.exit(0);
    }

    /**
     *
     * @param args arguments
     */
    public static void docheckout(String[] args) {
        if (args.length == 2) {
            CState currentstate = CState.fromFile();
            if (currentstate.getCurrentbranch().equals(args[1])) {
                exitWithError("No need to checkout the current branch.");
            } else if (!currentstate.getbranches().containsKey(args[1])) {
                exitWithError("No such branch exists.");
            } else {
                String branchname = args[1];
                String branchcommitid =
                        currentstate.getbranchwithname(branchname);
                String[] input = {"reset", branchcommitid};
                doreset(input, false);
                CState currentstate2 = CState.fromFile();
                currentstate2.setCurrentbranch(branchname);
                currentstate2.putbranch(branchname, branchcommitid);
                currentstate2.saveState();
            }

        } else if (args[1].equals("--") && args.length == 3) {
            String filename = args[2];
            CState currentstate = CState.fromFile();
            String csha1 = currentstate.getCommitsha1();
            Commit newestcommit = Commit.fromFile(csha1);
            if (newestcommit.getFiles().containsKey(filename)) {
                String sha1offile = newestcommit.getFiles().get(filename);
                File blobFile = Utils.join(blobfolder, sha1offile);
                File originFile = Utils.join(CWD, filename);
                String content = Utils.readContentsAsString(blobFile);
                Utils.writeContents(originFile, content);
            } else {
                exitWithError("File does not exist in that commit.");
            }
            currentstate.saveState();
        } else if (args[2].equals("--") && args.length == 4) {
            partcheckout(args);
        }
    }

    /**
     *
     * @param args arguments
     */
    public static void partcheckout(String[] args) {
        String commitid = args[1];
        String filename = args[3];
        List<String> dir = Utils.plainFilenamesIn(commitfolder);
        if (dir == null) {
            exitWithError("No commit with that id exists.");
        }
        if (commitid.length() < Utils.UID_LENGTH) {
            for (String ele: dir) {
                String sub = ele.substring(0, commitid.length());
                if (sub.equals(commitid)) {
                    commitid = ele;
                    break;
                }
            }
        }
        if (dir.contains(commitid)) {
            Commit commitfile = Commit.fromFile(commitid);
            List<String> allfilesworking = Utils.plainFilenamesIn(CWD);
            CState currentstate = CState.fromFile();
            String currentcommitsha1 = currentstate.getCommitsha1();
            Commit currentcommit = Commit.fromFile(currentcommitsha1);
            if (commitfile.getFiles().containsKey(filename)) {
                if (allfilesworking != null) {
                    for (String workingfile: allfilesworking) {
                        if (!currentcommit.getFiles().
                                containsKey(workingfile)) {
                            if (commitfile.getFiles().
                                    containsKey(workingfile)) {
                                exitWithError("There is an "
                                        + "untracked file in the way;"
                                        + " delete it or add it first.");
                            }
                        }
                    }
                }
                String sha1offile = commitfile.getFiles().get(filename);
                File blobFile = Utils.join(blobfolder, sha1offile);
                File originFile = Utils.join(CWD, filename);
                String content = Utils.readContentsAsString(blobFile);
                Utils.writeContents(originFile, content);
            } else {
                exitWithError("File does not exist in that commit.");
            }
        } else {
            exitWithError("No commit with that id exists.");
        }
    }

    /**
     *
     * @param args arguments
     */
    public static void dorm(String[] args) {
        boolean check = true;
        List<String> stagenames = Utils.plainFilenamesIn(stagefolder);
        if (stagenames != null && stagenames.contains(args[1])) {
            Utils.join(stagefolder, args[1]).delete();
            check = false;
        }
        CState currentstate = CState.fromFile();
        String csha1 = currentstate.getCommitsha1();
        Commit newestcommit = Commit.fromFile(csha1);
        if (newestcommit.getFiles().containsKey(args[1])) {
            check = false;
            currentstate.savermfiles(args[1],
                    newestcommit.getFiles().get(args[1]));
            File rmFile = Utils.join(rmfolder, args[1]);
            File originFile = Utils.join(CWD, args[1]);
            String blobcontent = newestcommit.getFiles().get(args[1]);
            String realcontent =
                    Utils.readContentsAsString(Utils.join(blobfolder,
                            blobcontent));
            Utils.writeContents(rmFile, realcontent);
            originFile.delete();
            currentstate.saveState();
        }
        if (check) {
            exitWithError("No reason to remove the file.");
        }
        currentstate.saveState();
    }

    /**
     *
     * @param args arguments
     */
    public static void dogloballog(String[] args) {
        List<String> commits = Utils.plainFilenamesIn(commitfolder);
        SimpleDateFormat format = new
                SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
        int amount = commits.size();
        int init = 0;
        for (String ele: commits) {
            Commit temp = Commit.fromFile(ele);
            System.out.println("===");
            System.out.println("commit " + temp.sha1());
            if (temp.getSubparent() != null) {
                System.out.println("Merge: "
                        + temp.getP().substring(0, 6)
                        + temp.getSubparent().substring(0, 6));
            }
            String formattime = format.format(temp.getDate());
            System.out.println("Date: " + formattime);
            System.out.println(temp.getMessage());
            init++;
            if (init != amount) {
                System.out.println();
            }

        }
    }

    /**
     *
     * @param args arguments
     */
    public static void dobranch(String[] args) {
        CState currentstate = CState.fromFile();
        if (currentstate.getbranches().containsKey(args[1])) {
            exitWithError("A branch with that name already exists.");
        }
        currentstate.putbranch(args[1]);
        currentstate.saveState();
        System.exit(0);
    }

    /**
     *
     * @param args arguments
     */
    public static void dofind(String[] args) {
        boolean check = true;
        List<String> commitfiles = Utils.plainFilenamesIn(commitfolder);
        for (String ele: commitfiles) {
            Commit temp = Commit.fromFile(ele);
            if (temp.getMessage().equals(args[1])) {
                System.out.println(temp.sha1());
                check = false;
            }
        }
        if (check) {
            exitWithError("Found no commit with that message.");
        }
        System.exit(0);
    }

    /**
     *
     * @param args arguments
     */
    public static void dostatus(String[] args) {
        System.out.println("=== Branches ===");
        CState currentstate = CState.fromFile();
        String currentbranch = currentstate.getCurrentbranch();
        Set<String> lst = currentstate.getbranches().keySet();
        int thesize = lst.size();
        String[] lstbranches = new String[thesize];
        int i = 0;
        for (String ele: lst) {
            lstbranches[i] = ele;
            i++;
        }

        Arrays.sort(lstbranches);
        for (String key : lstbranches) {
            if (!currentbranch.equals(key)) {
                System.out.println(key);
            } else {
                System.out.println("*" + key);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        List<String> stagefiles = Utils.plainFilenamesIn(stagefolder);
        if (stagefiles != null) {
            for (String ele: stagefiles) {
                System.out.println(ele);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        List<String> rmfiles = Utils.plainFilenamesIn(rmfolder);
        if (rmfiles != null) {
            for (String ele: rmfiles) {
                System.out.println(ele);
            }
        }

        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");

        System.out.println();
        System.out.println("=== Untracked Files ===");

    }

    /**
     *
     * @param args arguments
     */
    public static void dormbranch(String[] args) {
        CState currentstate = CState.fromFile();
        if (!currentstate.getbranches().containsKey(args[1])) {
            exitWithError("A branch with that name does not exist.");
        } else if (currentstate.getCurrentbranch().equals(args[1])) {
            exitWithError("Cannot remove the current branch.");
        } else {
            currentstate.removebranch(args[1]);
        }
        currentstate.saveState();
        System.exit(0);
    }

    /**
     *
     * @param args arguments
     * @param just nothing for checking
     */
    public static void doreset(String[] args, boolean just) {
        CState currentstate = CState.fromFile();
        String currentcommitsha1 = currentstate.getCommitsha1();
        Commit currentcommit = Commit.fromFile(currentcommitsha1);
        List<String> allfilesworking = Utils.plainFilenamesIn(CWD);
        List<String> allcommits = Utils.plainFilenamesIn(commitfolder);
        if (!allcommits.contains(args[1])) {
            exitWithError("No commit with that id exists.");
        }
        Commit givencommit = Commit.fromFile(args[1]);
        if (allfilesworking != null) {
            for (String workingfile: allfilesworking) {
                if (!currentcommit.getFiles().containsKey(workingfile)) {
                    if (givencommit.getFiles().containsKey(workingfile)) {
                        exitWithError("There is an untracked file in the way;"
                                + " delete it or add it first.");
                    }
                }
            }
        }
        for (String previous: currentcommit.getFiles().keySet()) {
            if (!givencommit.getFiles().containsKey(previous)) {
                File previousFile = Utils.join(CWD, previous);
                previousFile.delete();
            }
        }
        for (String originname: givencommit.getFiles().keySet()) {
            String blobname = givencommit.getFiles().get(originname);
            File blobFile = Utils.join(blobfolder, blobname);
            File originFile = Utils.join(CWD, originname);
            String content = Utils.readContentsAsString(blobFile);
            Utils.writeContents(originFile, content);
        }
        currentstate.setCommitsha1(args[1]);
        if (just) {
            currentstate.putbranch(currentstate.gcur(), args[1]);
        }
        List<String> temp1 = Utils.plainFilenamesIn(stagefolder);
        for (String ele: temp1) {
            File tf = Utils.join(stagefolder, ele);
            tf.delete();
        }
        stagefolder.delete();
        stagefolder.mkdir();
        currentstate.saveState();
    }

    /**
     *
     * @param args arguments
     */
    public static void somemergeerror(String[] args) {
        CState currentstate = CState.fromFile();
        List<String> rmfiles = Utils.plainFilenamesIn(rmfolder);
        List<String> stagefiles = Utils.plainFilenamesIn(stagefolder);
        if (rmfiles.size() != 0 || stagefiles.size() != 0) {
            exitWithError("You have uncommitted changes.");
        }
        if (!currentstate.getbranches().containsKey(args[1])) {
            exitWithError("A branch with that name does not exist.");
        }
    }

    /**
     *
     * @param args input arguments
     */
    public static void domerge(String[] args) {
        boolean checkconflict = false;
        CState currentstate = CState.fromFile();
        String headname = currentstate.getCurrentbranch();
        String head = currentstate.getCommitsha1();
        somemergeerror(args);
        if (headname.equals(args[1])) {
            exitWithError("Cannot merge a branch with itself.");
        }
        Commit headcommit = Commit.fromFile(currentstate.getCommitsha1());
        String branchid = currentstate.getbranches().get(args[1]);
        Commit branchcommit = Commit.fromFile(branchid);
        boolean lll = false;
        String leastans = headcommit.findleastans(branchid, lll);
        if (leastans.equals(branchid)) {
            exitWithError("Given branch is an "
                    + "ancestor of the current branch.");
        }
        if (leastans.equals(head)) {
            currentstate.setCommitsha1(leastans);
            currentstate.saveState();
            String[] temp = {"checkout", args[1]};
            docheckout(temp);
            exitWithError("Current branch fast-forwarded.");
        }
        Commit anscommit = Commit.fromFile(leastans);
        Set<String> headfiles = headcommit.getFiles().keySet();
        Set<String> ansfiles = anscommit.getFiles().keySet();
        Set<String> branchfiles = branchcommit.getFiles().keySet();
        checkconflict = partmerge(leastans, args, checkconflict);
        checkconflict = secondmerge(leastans, args, checkconflict);
        Date date = new Date();
        String[] commitmessage =
                mergemessage(currentstate.getCurrentbranch(), args);
        docommit(commitmessage, date, false,
                branchid, currentstate.getCommitsha1());
        if (checkconflict) {
            System.out.println("Encountered a merge conflict. ");
        }
    }

    /**
     *
     * @param leastans split point
     * @param args arguments
     * @param checkconflict checkcomflict need to be updated
     * @return new checkconflict
     */
    public static boolean secondmerge(String leastans,
                                      String[] args, boolean checkconflict) {
        CState currentstate = CState.fromFile();
        Commit headcommit = Commit.fromFile(currentstate.getCommitsha1());
        String branchid = currentstate.getbranches().get(args[1]);
        Commit branchcommit = Commit.fromFile(branchid);
        Commit anscommit = Commit.fromFile(leastans);
        Set<String> headfiles = headcommit.getFiles().keySet();
        Set<String> ansfiles = anscommit.getFiles().keySet();
        Set<String> branchfiles = branchcommit.getFiles().keySet();
        for (String ele: ansfiles) {
            if (headfiles.contains(ele)) {
                boolean A = checkmodi(headcommit, anscommit, ele);
                if (!A && !branchfiles.contains(ele)) {
                    String[] rmdata = {"rm", ele};
                    dorm(rmdata);
                }
                if (branchfiles.contains(ele)) {
                    boolean B = checkmodi(headcommit, branchcommit, ele);
                    boolean C = checkmodi(anscommit, branchcommit, ele);
                    if (A && B && C) {
                        doconflict(ele, headcommit, branchcommit);
                        checkconflict = true;
                    }
                } else {
                    if (A) {
                        doconflict(ele, headcommit, branchcommit);
                        checkconflict = true;
                    }
                }
            } else {
                boolean C = checkmodi(anscommit, branchcommit, ele);
                if (C) {
                    doconflict(ele, headcommit, branchcommit);
                    checkconflict = true;
                }
            }
        }
        return checkconflict;
    }

    /**
     *
     * @param leastans split point
     * @param args arguments
     * @param checkconflict checkcomflict need to be updated
     * @return new checkconflict
     */
    public static boolean partmerge(String leastans,
                                    String[] args, boolean checkconflict) {
        CState currentstate = CState.fromFile();
        Commit headcommit = Commit.fromFile(currentstate.getCommitsha1());
        String branchid = currentstate.getbranches().get(args[1]);
        Commit branchcommit = Commit.fromFile(branchid);
        Commit anscommit = Commit.fromFile(leastans);
        Set<String> headfiles = headcommit.getFiles().keySet();
        Set<String> ansfiles = anscommit.getFiles().keySet();
        Set<String> branchfiles = branchcommit.getFiles().keySet();
        for (String ele: branchfiles) {
            if (ansfiles.contains(ele)) {
                boolean A = checkmodi(anscommit, branchcommit, ele);
                if (headfiles.contains(ele)) {
                    boolean B = checkmodi(headcommit, anscommit, ele);
                    if (A && !B) {
                        String[] temp = {"checkout", branchid, "--", ele};
                        docheckout(temp);
                        String[] temp2 = {"add", ele};
                        doadd(temp2);
                    }
                }
            } else {
                if (!headfiles.contains(ele)) {
                    String[] temp = {"checkout", branchid, "--", ele};
                    docheckout(temp);
                    String[] temp2 = {"add", ele};
                    doadd(temp2);
                } else {
                    boolean B = checkmodi(headcommit, branchcommit, ele);
                    if (B) {
                        doconflict(ele, headcommit, branchcommit);
                        checkconflict = true;
                    }
                }
            }
        }
        return checkconflict;
    }

    /**
     *
     * @param branch current branch
     * @param args arguments
     * @return merge message
     */
    public static String[] mergemessage(String branch, String[] args) {
        String mergemessage = "Merged ";
        mergemessage += args[1];
        mergemessage += " into ";
        mergemessage += branch + ".";
        String[] commitmessage = {"commit", mergemessage};
        return commitmessage;
    }
    /**
     *
     * @param name the name of file
     * @param cbranch the current branch commit class
     * @param gbranch the given branch commit class
     */
    public static void doconflict(String name, Commit cbranch, Commit gbranch) {
        String cblob = cbranch.getFiles().get(name);
        String gblob = gbranch.getFiles().get(name);
        File cblobfile = Utils.join(blobfolder, cblob);
        List<String> filesinblob = Utils.plainFilenamesIn(blobfolder);
        String ccontent = Utils.readContentsAsString(cblobfile);
        String mergecontent = "<<<<<<< HEAD\n";
        mergecontent += ccontent;
        mergecontent += "=======\n";
        if (filesinblob.contains(gblob)) {
            File gblobfile = Utils.join(blobfolder, gblob);
            String gcontent = Utils.readContentsAsString(gblobfile);
            mergecontent += gcontent;
        }
        mergecontent += ">>>>>>>\n";
        File merge = Utils.join(stagefolder, name);
        File origin = Utils.join(CWD, name);
        Utils.writeContents(merge, mergecontent);
        Utils.writeContents(origin, mergecontent);
    }

    /** Return a boolean that to check whether file is modified or not.
     * @FIRSTCOMMIT the first commit class
     * @SECONDCOMMIT the second commit class
     * @FILENAME the name of the file in the commit
     */
    public static boolean checkmodi(Commit firstcommit,
                                    Commit secondcommit, String filename) {
        String firstsha1 = firstcommit.getFiles().get(filename);
        String secondsha1 = secondcommit.getFiles().get(filename);
        File firstblob = Utils.join(blobfolder, firstsha1);
        String firstcontent = Utils.readContentsAsString(firstblob);
        File secondblob = Utils.join(blobfolder, secondsha1);
        String secondcontent = Utils.readContentsAsString(secondblob);
        return !firstcontent.equals(secondcontent);
    }

}
