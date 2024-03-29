import static util.argmanager.argManager.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;

import util.argmanager.ArgNotFoundException;
import util.filemanager.*;
import static logme.logMe.*;

class gcodeUtils {

    static String[] behav = { """
            setMinX: <type:double>
            <help:Shifts the .gcode automatically so that the smallest X-Value is the one given here.>
            <default:0>""",
            """
                    setMinY: <type:double>
                    <help:Shifts the .gcode automatically so that the smallest Y-Value is the one given here.>
                    <default:50>""",
            """
                    setMaxX: <type:double>
                    <help:Shifts the .gcode automatically so that the biggest X-Value is the one given here.>""",
            """
                    setMaxY: <type:double>
                    <help:Shifts the .gcode automatically so that the biggest Y-Value is the one given here.>""",
            """
                    setToolOn: <type:String>
                    <help:Replaces the Tool On sequence>
                    <detailHelp:Replaces the Tool On command\nSyntax: --setToolOn [current ToolOn command] [new ToolOn command]>""",
            """
                    setToolOff: <type:String>
                    <help:Replaces the Tool Off sequence>
                    <detailHelp:Replaces the Tool Off command\nSyntax: --setToolOff [current ToolOff command] [new ToolOff command]>""",
            """
                    regToolOff: <type:String>
                    <help: Might be needed for optimisization if ToolOff/ToolOn can't be automatically detected>""",
            """
                    regToolOn: <type:String>
                    <help: Might be needed for optimisization if ToolOff/ToolOn can't be automatically detected>""",
            """
                    shiftX: <type:double>
                    <help:Shifts the whole .gcode the given amount of milimeters in the X-direction.>
                    <detailHelp:Shifts the whole .gcode the given amount of milimeters in the X-direction.\nSyntax:--shiftX [amount of mm to shift]>""",
            """
                    shiftY: <type:double>
                    <help:Shifts the whole .gcode the given amount of milimeters in the Y-direction.>
                    <detailHelp:Shifts the whole .gcode the given amount of milimeters in the Y-direction.\nSyntax:--shiftY [amount of mm to shift]>""",
            """
                    size: <type: double>
                    <help: Resizes the document with the given factor.>""",
            """
                    file:
                    <help:The path of the .gcode-file that shall be modified.>
                    <detailHelp:Insert the relative or full path to the .gcode file here that shall be modified. If the file does not exist or contains errors, you will be notified.>""",
            """
                    optimize: <type:boolean>
                    <help:Optimizes the paths by their starting point. Combines disconnected paths with same start/endpoints.>""",
            """
                    overwrite: <type:boolean>
                    <help:If true, the file will be overwritten>""",
            """
                    newFile: <type=String>
                    <help=If the file is NOT overwritten, you can choose a new file>
                    <detailHelp=If the file is NOT overwritten, you can choose a new file.
                    By default a \"_new\" is added or, if it is already a new file, an index is added or increased.>""",
            """
                    standardPrep: <type=Boolean>
                    <help=Applies a standard set of operations on the file in order to declutter the command \n         and use a file created by makelangelo without further tinkering.>""",
            """
                    genPrepGcode: <type=Boolean>
                    <help=Generates a gcode-file for setting up a pen for plotting as standardPrep leaves no time after homing to prepare.>""",
            """
                    textToGcode: <type=boolean>
                    <help=Generates gcode from the text given file.>""",
            """
                    textSize: <type=double> <default=3.3>
                    <help=Sets the size of the text in mm.>""",
    };

    static File sourceFile = new File("");

    // #region textToGcode
    static double lineHeight = 4;
    // #endregion

    // #region standardPrep
    static int bedSizeX = 300;
    static int bedSizeY = 300;
    static int toolOffsetX = 50;
    static int toolOffsetY = -45;
    static int toolOnHeight = 10;
    static int toolOffOffsetZ = 1;
    static String newToolOn = "";
    static String newToolOff = "";

    static double lowestX = 3;
    static double lowestY = 3;

    static String prepGcode = """
            ; Auto generated by gcodeUtils: https://github.com/LuaniMadh/gcodeUtils
            G28         ;   Homing
            %s          ;   Tool Off height
            G1 X50 Y50  ;   Move somewhere
            %s          ;   Tool On
            """;

    // #endregion

    final static double combineDist = 0.2;

    static ArrayList<String> gcode;
    static String ToolOn = "";
    static String ToolOff = "";

    public static void main(String[] args) {
        addAutoHeader(gcodeUtils.class, LOG_LEVEL.MAIN.header());
        run(args);
    }

    public static void run(String[] args) {
        setBehaviour(behav);
        parse(args);

        try {
            if (!isSet("genPrepGcode")) {
                if (isSet("file"))
                    sourceFile = new File(getArg("file"));
                else
                    sourceFile = Filemanager.chooseFile();

                log("Operating on file: " + sourceFile.getAbsolutePath());
                if (!sourceFile.exists()) {
                    error("File not found: " + sourceFile.getAbsolutePath());
                    System.exit(1);
                }
                loadFileProgressBar(getArg("file"));
            }
            String newFilename = getNewFilename();
            work();
            log("Saved to " + newFilename);
            Filemanager.saveFile(newFilename, gcode);
        } catch (Exception e) {
            error(e);
            error("Aborting...");
            System.exit(1);
        }
    }

    private static void loadFileProgressBar(String file) {
        String f = file;
        if (!new File(f).exists())
            f = Filemanager.chooseFile(new File("")).getAbsolutePath();

        if (new File(f).isDirectory() || !new File(f).exists())
            return;

        new Thread() {
            @Override
            public void run() {
                try {
                    gcode = Filemanager.getFileContentArrayList(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }.start();

        while (Filemanager.getProgress() != 1 || gcode == null || gcode.isEmpty()) {
            sameLineLog("Progress: " + Filemanager.getProgress() + "\r");
        }
        log("Finished loading file" + " ".repeat(50));
    }

    private static String getNewFilename() {
        try {
            // System.err.println("Please set a file to operate on!");
            // System.exit(0);
            String path = sourceFile.getParent();
            if (path == null)
                path = new File("").getAbsolutePath();
            String fullfilename = (isSet("file")) ? sourceFile.getName() : "prep.gcode";
            String oldFilename = fullfilename.substring(0, fullfilename.lastIndexOf("."));

            if (new File(path + "/" + fullfilename).exists() || new File(fullfilename).exists()) {
                if (isSet("newFile")) {
                    if (getArg(boolean.class, "overwrite")) {
                        return getArg("newFile");
                    } else {
                        if (new File(getArg("newFile")).exists()) {
                            throw new FileAlreadyExistsException(
                                    "newFile" + " - You can overwrite it by adding --overwrite");
                        } else {
                            return getArg("newFile");
                        }
                    }
                } else if (!getArg(Boolean.class, "overwrite")) {
                    String num = oldFilename.substring(oldFilename.indexOf("_") + 1, oldFilename.length());
                    // _new or _001
                    if (oldFilename.endsWith("_new")
                            && !(new File(oldFilename.substring(0, oldFilename.lastIndexOf("_") + 1) + "001.gcode")
                                    .exists())) {
                        return oldFilename.substring(0, oldFilename.indexOf("_") + 1) + "001.gcode";
                    } else if ((oldFilename.contains("_") && isInt(num))
                            || (new File(oldFilename.substring(0, oldFilename.indexOf("_") + 1) + "001.gcode").exists())
                            || (new File(oldFilename + "_new.gcode").exists())) {
                        if (!isInt(num)) {
                            num = "000";
                        }
                        if (!oldFilename.contains("_"))
                            oldFilename = oldFilename + "_";
                        int iteration = 1;
                        String fnBlock = oldFilename.substring(0, oldFilename.indexOf("_") + 1);
                        String fn = fnBlock + CapIntToLength4(Integer.parseInt(num) + iteration) + ".gcode";
                        while (new File(fn).exists()) {
                            fn = fnBlock + CapIntToLength4(Integer.parseInt(num) + iteration) + ".gcode";
                            iteration++;
                        }
                        return fn;
                    } else {
                        return oldFilename + "_new.gcode";

                    }
                } else {
                    return fullfilename;
                }
            } else if (isSet("genPrepGcode")) {
                return fullfilename;
            } else {
                error(sourceFile.getName() + " does not exist.");
                throw new FileNotFoundException();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return "ERRORgcode.txt";
    }

    private static boolean isInt(String i) {
        try {
            Integer.parseInt(i);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static String CapIntToLength4(int i) {
        if (i >= 100) {
            return "" + i;
        } else if (i >= 10) {
            return "0" + i;
        } else {
            return "00" + i;
        }
    }

    public static void work() {
        try {
            boolean somethingDone = false;
            if (isSet("textToGcode")) {
                stdToolOnOff();
                textToGcode.loadChars();
                // Home, Tool off
                ArrayList<String> text = gcode;
                gcode = new ArrayList<String>();
                gcode.add("G28");
                gcode.add(ToolOff);

                double cursor_x = 0;
                double cursor_y = 0;
                for (String line : text) {
                    for (char c : line.toCharArray()) {
                            var charGcode = textToGcode.getGcode(c, cursor_x, cursor_y);
                            gcode.addAll(charGcode.gcode());
                            cursor_x += charGcode.advance();
                    }
                    cursor_y -= lineHeight;
                    cursor_x = 0;
                }
                somethingDone = true;
            }
            if (isSet("genPrepGcode")) {
                stdToolOnOff();
                gcode = strToList(String.format(prepGcode, ToolOff, ToolOn));
                return;
            }
            if (isSet("standardPrep")) {
                double maxX = findMaxVal("X");
                double minY = findMinVal("Y");
                shift("X", bedSizeX - toolOffsetX - maxX - lowestX);
                shift("Y", -toolOffsetY - minY + lowestY);
                newToolOn = "G0 Z" + toolOnHeight;
                newToolOff = "G0 Z" + (toolOnHeight + toolOffOffsetZ);
                optimize();
                setTool();
                // find G28(Home) and add ToolOff afterwards
                for (int i = 0; i < gcode.size(); i++)
                    if (gcode.get(i).startsWith("G28")) {
                        gcode.add(i + 1, newToolOff);
                        break;
                    }
                // remove breaking
                for (int i = 0; i < gcode.size(); i++)
                    if (gcode.get(i).startsWith("M0")) {
                        gcode.remove(i);
                        break;
                    }
                somethingDone = true;
            }
            if (isSet("setMinX")) {
                double minVal = findMinVal("X");
                shift("X", (getArg(Double.class, "setMinX") - minVal));
                somethingDone = true;
            }
            if (isSet("setMinY")) {
                double minVal = findMinVal("Y");
                shift("Y", (getArg(Double.class, "setMinY") - minVal));
                somethingDone = true;
            }
            if (isSet("setMaxX")) {
                double maxVal = findMaxVal("X");
                shift("X", (getArg(Double.class, "setMaxX") - maxVal));
                somethingDone = true;
            }
            if (isSet("setMaxY")) {
                double maxVal = findMaxVal("Y");
                shift("Y", (getArg(Double.class, "setMaxY") - maxVal));
                somethingDone = true;
            }
            if (isSet("setToolOn") || isSet("setToolOff")) {
                setTool();
                somethingDone = true;
            }
            if (isSet("shiftX") || isSet("shiftY")) {
                if (isSet("shiftX")) {
                    shift("X", getArg(double.class, "shiftX"));
                }
                if (isSet("shiftY")) {
                    shift("Y", getArg(double.class, "shiftY"));
                }
                somethingDone = true;
            }
            if (getArg(Boolean.class, "optimize")) {
                optimize();
                somethingDone = true;
            }
            if (!somethingDone) {
                log("Please select a command. For further information use --help.");
            }
        }catch(

    Exception e)
    {
        e.printStackTrace();
    }
    }

    private static void stdToolOnOff() {
        if (isSet("setToolOn"))
            ToolOn = getArg("setToolOn");
        else {
            ToolOn = "G0 Z" + toolOnHeight;
        }
        if (isSet("setToolOff"))
            ToolOn = getArg("setToolOff");
        else {
            ToolOff = "G0 Z" + (toolOnHeight + toolOffOffsetZ);
        }
    }

    public static void setTool() {
        try {
            autoGetToolOnOff();
            ToolOn = ((isSet("regToolOn")) ? getArg("regToolOn") : ToolOn);
            ToolOff = ((isSet("regToolOff")) ? getArg("regToolOff") : ToolOff);

            boolean repToolOn = !ToolOn.isBlank();
            boolean repToolOff = !ToolOff.isBlank();
            int replacedToolOn = 0;
            int replacedToolOff = 0;
            String ntOn = (isSet("setToolOn") ? getArg("setToolOn") : newToolOn);
            String ntOff = (isSet("setToolOff") ? getArg("setToolOff") : newToolOff);
            ArrayList<String> newGcode = new ArrayList<String>();
            for (String s : gcode) {
                if (repToolOn && s.contains(ToolOn)) {
                    newGcode.add(s.replace(ToolOn, ntOn));
                    replacedToolOn++;
                    continue;
                }
                if (repToolOff && s.contains(ToolOff)) {
                    newGcode.add(s.replace(ToolOff, ntOff));
                    replacedToolOff++;
                    continue;
                }
                newGcode.add(s);
            }
            if (isSet("setToolOn")) {
                log("Replaced " + replacedToolOn + " times ToolOn");
            }
            if (isSet("setToolOff")) {
                log("Replaced " + replacedToolOff + " times ToolOff");
            }
            gcode = newGcode;
        } catch (ArgNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static double findMinVal(String Letter) {
        double minVal = Double.MAX_VALUE;
        int i = 0;
        for (String s : gcode) {
            i++;
            if(s.contains("\n"))
                log("Line " + i + " contains a linebreak. This may cause problems: " + s + "\n");
            // if(i%100 == 0)
            if (s == null)
                continue;
            if (s.contains(Letter)) {
                int startIndex = s.indexOf(Letter) + 1;
                int comment = s.indexOf(";");
                if (comment == -1)
                    comment = s.length();
                if (startIndex >= comment)
                    break;
                int endIndex = s.indexOf(" ", s.indexOf(Letter));
                if (endIndex >= comment)
                    endIndex = comment;
                if (endIndex == -1) {
                    endIndex = s.length();
                }
                String sub = s.substring(startIndex, endIndex);
                double v = Double.parseDouble(sub);
                if (v < minVal) {
                    minVal = v;
                }
            }
        }
        return minVal;
    }

    public static double findMaxVal(String Letter) {
        double maxVal = 0;
        for (String s : gcode) {
            if (s.contains(Letter)) {
                int startIndex = s.indexOf(Letter) + 1;
                int comment = s.indexOf(";");
                if (comment == -1)
                    comment = s.length();
                if (startIndex >= comment)
                    break;
                int endIndex = s.indexOf(" ", s.indexOf(Letter));
                if (endIndex >= comment || endIndex == -1)
                    endIndex = comment;
                String sub = s.substring(startIndex, endIndex);
                double v = Double.parseDouble(sub);
                if (v > maxVal) {
                    maxVal = v;
                }
            }
        }
        return maxVal;
    }

    public static void shift(String Letter, double mm) {
        for (int i = 0; i < gcode.size(); i++) {
            String s = gcode.get(i);
            if (s == null)
                continue;
            if (s.contains(Letter)) {
                int startIndex = s.indexOf(Letter) + 1;
                int comment = s.indexOf(";");
                if (comment == -1)
                    comment = s.length();
                if (startIndex >= comment)
                    break;
                int endIndex = s.indexOf(" ", s.indexOf(Letter));
                if (endIndex >= comment)
                    endIndex = comment;
                if (endIndex == -1)
                    endIndex = s.length();
                String sub = s.substring(startIndex, endIndex);
                double v = Double.parseDouble(sub);
                double nv = v += mm;
                s = s.substring(0, startIndex) + capString(nv, 8) + s.substring(endIndex, s.length());
            }
            gcode.set(i, s);
        }
        log("Gcode shifted " + mm + "mm in " + Letter + " direction.");
    }

    static String capString(Object o, int length) {
        return capString("" + o, length);
    }

    /**
     * Cuts a string to a given length and removes trailing zeros
     * 
     * @param length
     * @return
     */
    static String capString(String str, int length) {
        if (str.length() > length) {
            str = str.substring(0, length);
        }
        if (str.contains(".")) {
            while (str.endsWith("0")) {
                str = str.substring(0, str.length() - 1);
            }
            if (str.endsWith(".")) {
                str = str.substring(0, str.length() - 1);
            }
        }
        return str;
    }

    /**
     * If the gcode is formatted right (and the gcode is longer than the start and
     * end sequence)the following should work:
     * A "cursor"(drop) is dropped in the middle of the doc
     * If this is a path the line should start with G1
     * Otherwise it should be G0 or something else
     * In this case the line before that is used and tested the same way
     * From this line there should be a Tool On at the beginning and a Tool Off at
     * the end of the row of G1s/the path
     * 
     * NOTE: this currently only works with single-line tool offs/ons
     * NOTE: if the gcode has a wrong format everything keeps the state it had
     * before. If you want to check for the right format use isInFormat()
     */
    public static void autoGetToolOnOff() {
        // Find drop
        int drop = (int) ((double) gcode.size() / 2.0);
        boolean foundDrop = false;
        if (!gcode.get(drop).startsWith("G1")) {
            for (int i = drop; i > 1; i--) {
                if (gcode.get(i).startsWith("G1")) {
                    drop = i;
                    foundDrop = true;
                    break;
                }
            }
            if (!foundDrop) {
                for (int i = drop; i < gcode.size() - 1; i++) {
                    if (gcode.get(i).startsWith("G1")) {
                        drop = i;
                        foundDrop = true;
                        break;
                    }
                }
                if (!foundDrop) {
                    // No drop found
                    log("Couldn't find a drop");
                    return;
                }
            }
        } else {
            foundDrop = true;
        }

        boolean gotNewToolOn = false;
        boolean gotNewToolOff = false;
        // Tool on
        for (int i = drop; i > 0; i--) {
            if (!gcode.get(i).startsWith("G1")) {
                gotNewToolOn = true;
                ToolOn = "" + gcode.get(i);
                break;
            }
        }
        // Tool off
        for (int i = drop; i < gcode.size(); i++) {
            if (!gcode.get(i).startsWith("G1")) {
                gotNewToolOff = true;
                ToolOff = "" + gcode.get(i);
                break;
            }
        }
        if (!gotNewToolOff || !gotNewToolOn) {
            log("Could not auto-get ToolOn/ToolOff. " + "\nDrop: " + drop);
        }
        log("Automatically found ToolOn & ToolOff: " + ToolOn + ", " + ToolOff);
    }

    /**
     * https://sameer.github.io/svg2gcode/ or https://github.com/sameer/svg2gcode
     * Format:
     * Start sequence
     * Tool off
     * For each path:
     * Movement to the beginning of the path
     * tool on
     * the path
     * Tool off
     * End sequence
     * 
     * @return
     */
    public static boolean isInFormat() {
        autoGetToolOnOff();
        if (ToolOff.isEmpty() || ToolOn.isEmpty()) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static void optimize() {
        // Getpaths && toolOn && toolOff
        try {
            log("Optimizing");
            if (!isInFormat()) {
                log(
                        """
                                Not in right format:
                                        Start sequence
                                        Tool off
                                        For each path:
                                            Movement to the beginning of the path
                                            tool on
                                            the path
                                            Tool off
                                        End sequence""");
                return;
            }
            if ((isSet("regToolOn") || isSet("regToolOff"))
                    && !(isSet("regToolOn") && isSet("regToolOff"))) {
                log("Please set both Tool On and Tool Off. One isn't enough.");
                return;
            }
            if (isSet("setToolOn") || isSet("setToolOff")) {
                autoGetToolOnOff();
            }
        } catch (ArgNotFoundException e) {
            e.printStackTrace();
        }

        // Sortpaths
        // find beginning
        // Start Sequence
        // Tool Off
        // Movement
        // ...
        int startOfFirstPath = -1;
        int endOfLastPath = -1;
        for (int i = 0; i < gcode.size(); i++) {

            if (gcode.get(i).startsWith(ToolOn)) {
                startOfFirstPath = i - 1;
                break;
            }
        }
        for (int i = gcode.size() - 1; i > 0; i--) {
            if (gcode.get(i).startsWith(ToolOff)) {
                endOfLastPath = i + 1;
                break;
            }
        }
        if (startOfFirstPath == -1 || endOfLastPath == -1) {
            log("Could't find end/start of the paths");
            return;
        }

        /*
         * Movement to the beginning of the path
         * tool on
         * the path
         * Tool off
         */
        enum structure {
            movement,
            toolOn,
            path,
            toolOff
        }
        structure status = structure.movement;

        ArrayList<gcodepath> paths = new ArrayList<gcodepath>();

        double buffStartX = -1;
        double buffStartY = -1;
        double buffEndX = -1;
        double buffEndY = -1;
        ArrayList<String> buffContent = new ArrayList<String>();
        // Get/Load all paths from the gcode in gcodepaths
        for (int i = startOfFirstPath; i < endOfLastPath; i++) {
            String line = gcode.get(i);
            switch (status) {
                case movement:
                    buffStartX = Double
                            .parseDouble(line.substring(line.indexOf("X") + 1, line.indexOf(" ", line.indexOf("X"))));
                    int syend = line.indexOf(" ", line.indexOf("Y"));// StartYEND = syend
                    buffStartY = Double
                            .parseDouble(line.substring(line.indexOf("Y") + 1, (syend == -1) ? line.length() : syend));
                    status = structure.toolOn;
                    break;
                case toolOn:
                    // Do nothing. you could only check for errors in the structure
                    status = structure.path;
                    break;
                case path:
                    if (line.equals(ToolOff)) {
                        status = structure.toolOff;
                        String oldLine = gcode.get(i - 1);
                        buffEndX = Double.parseDouble(oldLine.substring(oldLine.indexOf("X") + 1,
                                oldLine.indexOf(" ", oldLine.indexOf("X"))));
                        int eyend = oldLine.indexOf(" ", oldLine.indexOf("Y"));// EndYEND = eyend
                        int comment = oldLine.indexOf(";", oldLine.indexOf("Y"));// EndYEND = eyend
                        if (comment != -1 && (eyend > comment || eyend == -1))
                            eyend = comment;
                        buffEndY = Double.parseDouble(
                                oldLine.substring(oldLine.indexOf("Y") + 1, (eyend == -1) ? oldLine.length() : eyend));

                        paths.add(new gcodepath(buffStartX, buffStartY, buffEndX, buffEndY,
                                (ArrayList<String>) buffContent.clone()));

                        buffStartX = -1;
                        buffStartY = -1;
                        buffEndX = -1;
                        buffEndY = -1;
                        buffContent = new ArrayList<String>();
                        status = structure.movement;
                    } else {
                        buffContent.add(line);
                    }
                    break;
                case toolOff:
                    // This should never be
                    log("Something's wrong");
                    break;
            }
        }

        ArrayList<gcodepath> sortedPaths = new ArrayList<gcodepath>();
        double currentX = 0;
        double currentY = 0;
        while (!paths.isEmpty()) {
            double lowestDistance = distance(currentX, currentY, paths.get(0).startX(), paths.get(0).startY());
            boolean lowestDistanceReverse = false;
            gcodepath nextPath = paths.get(0);
            for (gcodepath gcp : paths) {
                boolean reverse = false;
                double dist = distance(currentX, currentY, gcp.startX(), gcp.startY());
                double rDist = distance(currentX, currentY, gcp.endX(), gcp.endY());
                if (rDist < dist) {
                    dist = rDist;
                    reverse = true;
                }
                if (dist < lowestDistance) {
                    lowestDistance = dist;
                    nextPath = gcp;
                    lowestDistanceReverse = reverse;
                }
                if (lowestDistance == 0) {
                    break;
                }
            }
            paths.remove(nextPath);
            if (lowestDistance <= combineDist && !sortedPaths.isEmpty()) {
                gcodepath previous = sortedPaths.remove(sortedPaths.size() - 1);
                gcodepath ncombined = previous.combine(nextPath, lowestDistanceReverse);
                sortedPaths.add(ncombined);
            } else
                sortedPaths.add(lowestDistanceReverse ? nextPath.reverse() : nextPath);
            if (!lowestDistanceReverse) {
                currentX = nextPath.endX();
                currentY = nextPath.endY();
            } else {
                currentX = nextPath.startX();
                currentY = nextPath.startY();
            }

        }

        ArrayList<String> newGcode = new ArrayList<String>();
        for (int i = 0; i < startOfFirstPath; i++) {
            newGcode.add(gcode.get(i));
        }
        for (gcodepath gcp : sortedPaths) {
            newGcode.add(gcp.startLine());
            newGcode.add(ToolOn);
            for (String s : gcp.content()) {
                newGcode.add(s);
            }
            newGcode.add(ToolOff);
        }
        for (int i = endOfLastPath; i < gcode.size(); i++) {
            newGcode.add(gcode.get(i));
        }
        gcode = newGcode;
    }

    static double distance(double aX, double aY, double bX, double bY) {
        return Math.sqrt((Math.pow((aX - bX), 2)) + Math.pow((aY - bY), 2));
    }

    private record gcodepath(double startX, double startY, double endX, double endY, ArrayList<String> content) {

        private String firstLine() {
            return "G0 X" + startX + " Y" + startY;
        }

        public String toString() {
            String returning = firstLine() + "\n" + ToolOn;
            for (String s : content) {
                returning += "\n" + s;
            }
            returning += "\n" + ToolOff;
            return returning;
        }

        public String startLine() {
            return "G0 X" + startX + " Y" + startY;
        }

        /**
         * If reverse is true, the second path will be reversed
         * 
         * @param gcp
         * @param reverse
         * @return
         */
        public gcodepath combine(gcodepath gcp, boolean reverse) {
            ArrayList<String> nc = new ArrayList<String>();
            // nc.addAll(reverse?reverseList(content):content);
            // nc.addAll(gcp.content());
            // var res = new gcodepath(reverse?endX:startX, reverse?endY:startY, gcp.endX(),
            // gcp.endY(), nc);
            nc.addAll(content);
            if (!reverse)
                nc.add("G1 X" + gcp.startX() + " Y" + gcp.startY());
            nc.addAll(reverse ? reverseList(gcp.content) : gcp.content());
            if (reverse)
                nc.add("G1 X" + gcp.startX() + " Y" + gcp.startY());
            var res = new gcodepath(startX, startY, reverse ? gcp.startX() : gcp.endX(),
                    reverse ? gcp.startY() : gcp.endY(), nc);
            return res;
        }

        public gcodepath reverse() {
            ArrayList<String> nc = new ArrayList<String>(content.size() + 1);
            nc.addAll(reverseList(content));
            nc.add("G1 X" + startX + " Y" + startY);
            return new gcodepath(endX, endY, startX, startY, nc);
        }
    };

    private static <T> ArrayList<T> reverseList(ArrayList<T> l) {
        ArrayList<T> res = new ArrayList<T>(l.size());
        for (int i = l.size() - 1; i >= 0; i--) {
            res.add(l.get(i));
        }
        return res;
    }

    public static ArrayList<String> strToList(String str) {
        ArrayList<String> res = new ArrayList<>();
        for (String s : str.split("\n"))
            res.add(s);
        return res;
    }
}