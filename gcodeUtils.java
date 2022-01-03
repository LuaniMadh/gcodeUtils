import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;

import javax.tools.Tool;

import argmanager.ArgNotFoundException;

import static argmanager.argManager.*;
import filemanager.*;

class gcodeUtils {

    static ArrayList<String> gcode;
    static String ToolOn = "";
    static String ToolOff = "";

    public static void main(String[] args) {
       run(args);
    }

    public static void run(String[] args){
        setBehaviour(
            "setMinX: <type:double> <help:Shifts the .gcode automatically so that the smallest X-Value is the one given here.>",
            "setMinY: <type:double> <help:Shifts the .gcode automatically so that the smallest Y-Value is the one given here.>",
            "setMaxX: <type:double> <help:Shifts the .gcode automatically so that the biggest X-Value is the one given here.>",
            "setMaxY: <type:double> <help:Shifts the .gcode automatically so that the biggest Y-Value is the one given here.>",
            "setToolOn: <type:String> <help:Replaces the Tool On sequence> <detailHelp:Replaces the Tool On command\nSyntax: --setToolOn [current ToolOn command] [new ToolOn command]>",
            "setToolOff: <type:String> <help:Replaces the Tool Off sequence> <detailHelp:Replaces the Tool Off command\nSyntax: --setToolOff [current ToolOff command] [new ToolOff command]>",
            "regToolOff: <type:String> <help: Might be needed for optimisization if ToolOff/ToolOn can't be automatically detected>",
            "regToolOn: <type:String> <help: Might be needed for optimisization if ToolOff/ToolOn can't be automatically detected>",
            "shiftX: <type:double> <help:Shifts the whole .gcode the given amount of milimeters in the X-direction.> <detailHelp:Shifts the whole .gcode the given amount of milimeters in the Y-direction.\nSyntax:--shiftX [amount of mm to shift]>",
            "shiftY: <type:double> <help:Shifts the whole .gcode the given amount of milimeters in the Y-direction.> <detailHelp:Shifts the whole .gcode the given amount of milimeters in the Y-direction.\nSyntax:--shiftY [amount of mm to shift]>",
            "size: <type: double> <help: Resizes the document with the given factor.>",
            "file: <help:The path of the .gcode-file that shall be modified.> <detailHelp:Insert the relative or full path to the .gcode file here that shall be modified. If the file does not exist or contains errors, you will be notified.>",
            "optimize: <type:boolean> <help:Optimizes the paths by their starting point>",
            "overwrite: <type:boolean> <help:If true, the file will be overwritten>",
            "newFile: <type=String> <help=If the file is NOT overwritten, you can choose a new file> <detailHelp=If the file is NOT overwritten, you can choose a new file.\nBy default a \"_new\" is added or, if it is already a new file, an index is added or increased.>");
    parse(args);
    try {
        String newFilename = getNewFilename();
        new Thread() {
            @Override
            public void run() {
                try {
                    gcode = Filemanager.getFileContentArrayList(getArg("file"));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }.start();

        while (Filemanager.getProgress() != 1 || gcode == null || gcode.isEmpty()) {
            System.out.print("Progress: " + Filemanager.getProgress() + "\r");
        }
        System.out.println("Finished loading file                                                                ");
        work();
        Filemanager.saveFile(newFilename, gcode);
    } catch (Exception e) {
        e.printStackTrace();
    }
    }

    private static String getNewFilename() {
        try {

            if (!isSet("file")) {
                System.err.println("Please set a file to operate on!");
                System.exit(0);
            }

            String fullfilename = getArg("file");
            String oldFilename = fullfilename.substring(0, fullfilename.lastIndexOf("."));

            if (new File(getArg("file")).exists()) {
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
                    if (oldFilename.endsWith("_new") && !(new File(oldFilename.substring(0, oldFilename.lastIndexOf("_") + 1) + "001.gcode").exists())) {
                        return oldFilename.substring(0, oldFilename.indexOf("_") + 1) + "001.gcode";
                    } else if ((oldFilename.contains("_") && isInt(num)) || (new File(oldFilename.substring(0, oldFilename.indexOf("_") + 1) + "001.gcode").exists()) || (new File(oldFilename + "_new.gcode").exists())) {
                        if(!isInt(num)){
                            num = "000";
                        }
                        if(!oldFilename.contains("_"))
                            oldFilename = oldFilename + "_";
                        int iteration = 1;
                        String fnBlock = oldFilename.substring(0, oldFilename.indexOf("_") + 1);
                        String fn = fnBlock + AllDigitsOfInt(Integer.parseInt(num) + iteration) + ".gcode";
                        while(new File(fn).exists()){
                            fn = fnBlock + AllDigitsOfInt(Integer.parseInt(num) + iteration) + ".gcode";
                            iteration++;
                        }
                        return fn;
                    } else {
                        return oldFilename + "_new.gcode";

                    }
                } else {
                    return getArg("file");
                }
            } else {
                System.err.println(getArg("file"));
                throw new FileNotFoundException();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
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

    private static String AllDigitsOfInt(int i) {
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
            if (isSet("setMinX")) {
                double minVal = findMinVal("X");
                shift("X", (getArg(Double.class, "setMinX") - minVal));
            } else if (isSet("setMinY")) {
                double minVal = findMinVal("Y");
                shift("Y", (getArg(Double.class, "setMinY") - minVal));
            } else if (isSet("setMaxX")) {
                double maxVal = findMaxVal("X");
                shift("X", (getArg(Double.class, "setMaxX") - maxVal));
            } else if (isSet("setMaxY")) {
                double maxVal = findMaxVal("Y");
                shift("Y", (getArg(Double.class, "setMaxY") - maxVal));
            } else if (isSet("setToolOn") || isSet("setToolOff")) {
                setTool();
            } else if (isSet("shiftX") || isSet("shiftY")) {
                if (isSet("shiftX")) {
                    shift("X", getArg(Integer.class, "shiftX"));
                }
                if (isSet("shiftY")) {
                    shift("Y", getArg(Integer.class, "shiftY"));
                }
            } else if (getArg(Boolean.class, "optimize")) {
                optimize();
            } else {
                System.out.println("Please select a command. For further information use --help.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setTool() {
        try {
            autoGetToolOnOff();
            ToolOn = ((isSet("regToolOn")) ? getArg("regToolOn") : ToolOn);
            ToolOff = ((isSet("regToolOff")) ? getArg("regToolOff") : ToolOff);

            boolean repToolOn = isSet("setToolOn");
            boolean repToolOff = isSet("setToolOff");
            int replacedToolOn = 0;
            int replacedToolOff = 0;
            String newToolOn = (isSet("setToolOn") ? getArg("setToolOn") : "");
            String newToolOff = (isSet("setToolOff") ? getArg("setToolOff") : "");
            ArrayList<String> newGcode = new ArrayList<String>();
            for (String s : gcode) {
                if (repToolOn && s.contains(ToolOn)) {
                    newGcode.add(s.replace(ToolOn, newToolOn));
                    replacedToolOn++;
                    continue;
                }
                if (repToolOff && s.contains(ToolOff)) {
                    newGcode.add(s.replace(ToolOff, newToolOff));
                    replacedToolOff++;
                    continue;
                }
                newGcode.add(s);
            }
            if (isSet("setToolOn")) {
                System.out.println("Replaced " + replacedToolOn + " times ToolOn");
            }
            if (isSet("setToolOff")) {
                System.out.println("Replaced " + replacedToolOff + " times ToolOff");
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
                if (endIndex >= comment)
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
        System.out.println(mm);
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
                s = s.substring(0, startIndex) + nv + s.substring(endIndex, s.length());
            }
            gcode.set(i, s);
        }
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
        int drop = gcode.size() / 2;
        boolean foundDrop = false;
        if (!gcode.get(drop).startsWith("G1")) {
            for (int i = drop; i > 1; i--) {
                if (gcode.get(drop).startsWith("G1")) {
                    drop = i;
                    foundDrop = true;
                    break;
                }
            }
            if (!foundDrop) {
                for (int i = drop; i < gcode.size() - 1; i++) {
                    if (gcode.get(drop).startsWith("G1")) {
                        drop = i;
                        foundDrop = true;
                        break;
                    }
                }
                if (!foundDrop) {
                    // No drop found
                    return;
                }
            }
        } else {
            foundDrop = true;
        }

        // Tool on
        for (int i = drop; i > 0 && ToolOn.isEmpty(); i--) {
            if (!gcode.get(i).startsWith("G1")) {
                ToolOn = gcode.get(i);
            }
        }
        for (int i = drop; i < gcode.size() && ToolOff.isEmpty(); i++) {
            if (!gcode.get(i).startsWith("G1")) {
                ToolOff = gcode.get(i);
            }
        }

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
        // String TOff = ToolOff;
        // String TOn = ToolOn;
        autoGetToolOnOff();
        if (ToolOff.isEmpty() || ToolOn.isEmpty()) {
            return false;
        }
        return true;
    }

    public static void optimize() {
        // Getpaths && toolOn && toolOff
        try {
            if (!isInFormat()) {
                System.out.println(
                        "Not in right format: \nStart sequence\nTool off\nFor each path:\n    Movement to the beginning of the path\n    tool on\n    the path\n    Tool off\nEnd sequence");
                return;
            }
            if ((isSet("regToolOn") || isSet("regToolOff"))
                    && !(isSet("regToolOn") && isSet("regToolOff"))) {
                System.out.println("Please set both Tool On and Tool Off. One isn't enough.");
                return;
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
            System.out.println("Could't find end/start of the paths");
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
                    System.out.println("Something's wrong");
                    break;
            }
        }

        ArrayList<gcodepath> sortedPaths = new ArrayList<gcodepath>();
        double currentX = 0;
        double currentY = 0;
        while (!paths.isEmpty()) {
            double lowestDistance = distance(currentX, currentY, paths.get(0).startX(), paths.get(0).startY());
            gcodepath nextPath = paths.get(0);
            for (gcodepath gcp : paths) {
                double dist = distance(currentX, currentY, gcp.startX(), gcp.startY());
                if (dist < lowestDistance) {
                    lowestDistance = dist;
                    nextPath = gcp;
                }
                if (lowestDistance == 0) {
                    break;
                }
            }
            paths.remove(nextPath);
            sortedPaths.add(nextPath);
            currentX = nextPath.endX();
            currentY = nextPath.endY();
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
        public String toString() {
            String returning = "G0 X" + startX + " Y" + startY + "\n" + ToolOn;
            for (String s : content) {
                returning += "\n" + s;
            }
            returning += "\n" + ToolOff;
            return returning;
        }

        public String startLine() {
            return "G0 X" + startX + " Y" + startY;
        }
    };
}