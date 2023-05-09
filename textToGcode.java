import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import static logme.logMe.*;
import static util.argmanager.argManager.*;

class textToGcode {

    private static double scale = 1.0;

    private static List<gchar> chars;

    public static void loadChars() {
        scale = getArg(Double.class, "textSize") / 800.0;

        // load the chars from the xml file
        if (chars == null)
            chars = new LinkedList<>();
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc;
            doc = builder.parse(new File("EMSReadability.svg"));
            doc.getDocumentElement().normalize();

            NodeList charsList = doc.getElementsByTagName("glyph");
            for (int i = 0; i < charsList.getLength(); i++) {
                Node charNode = charsList.item(i);
                gchar nC = new gchar(charNode);
                if (!nC.hasError())
                    chars.add(nC);
            }

        } catch (SAXException | IOException | ParserConfigurationException e) {
            error(e);
            error("Could not load chars");
        }
    }

    private static class gchar {
        private List<move> moves = new LinkedList<>();
        private String unicode;
        private String name;
        private boolean error = false;
        private double x_adv = 0;

        public boolean hasError() {
            return error;
        }

        public gchar(Node node) {
            try {
                NamedNodeMap attrs = node.getAttributes();
                name = attrs.getNamedItem("glyph-name").getNodeValue();
                unicode = attrs.getNamedItem("unicode").getNodeValue();
                x_adv = Double.parseDouble(attrs.getNamedItem("horiz-adv-x").getNodeValue()) * scale;
                if (unicode.length() > 1) {
                    String s = unicode;
                    unicode = Character.toString(Integer.parseInt(unicode, 16));
                    log(s + " " + unicode);
                }
                // "M 195 674 L 195 192 M 173 69.3 L 173 18.9 L 220 18.9 L 220 69.3 L 173 69.3"
                if (attrs.getNamedItem("d") != null) {
                    String m = attrs.getNamedItem("d").getNodeValue().strip();
                    List<move> mv = new LinkedList<>();
                    int lastFound = 0;
                    for (int i = 1; i <= m.length(); i++) {
                        String str = "";
                        if (i == m.length()) {
                            str = m.substring(lastFound, i).strip();
                        } else if (m.charAt(i) == 'M' || m.charAt(i) == 'L') {
                            str = m.substring(lastFound, i - 1).strip();
                            lastFound = i;
                        }
                        if (str.length() > 0)
                            mv.add(new move(str));
                    }
                    moves.add(mv.get(0));
                    moves.add(move.toolOn);
                    for (int i = 1; i < mv.size(); i++) {
                        if (mv.get(i).type() == 'M') {
                            moves.add(move.toolOff);
                            moves.add(mv.get(i));
                            moves.add(move.toolOn);
                        } else {
                            moves.add(mv.get(i));
                        }
                    }
                    moves.add(move.toolOff);
                }
            } catch (Exception e) {
                // error(e);
                // error("Could not load char " + name + " " + unicode);
            }
        }

        public ArrayList<String> toString(double x, double y, double scale) {
            ArrayList<String> ret = new ArrayList<String>(moves.size());
            for (move m : moves)
                ret.add(m.toString(x, y, scale));
            return ret;
        }

        private record move(char type, double x, double y) {
            public move(String str) {
                this(str.charAt(0),
                        Double.parseDouble(str.substring(1).split(" ")[1]),
                        Double.parseDouble(str.substring(1).split(" ")[2]));
            }

            public String toString(double fx, double fy, double scale) {
                if (type == 'O')
                    return gcodeUtils.ToolOff;
                if (type == 'I')
                    return gcodeUtils.ToolOn;
                return ((type == 'M') ? "G0" : "G1") +
                        " X" + gcodeUtils.capString(scale * x + fx, 8)
                        + " Y" + gcodeUtils.capString(scale * y + fy, 8);
            }

            public static move toolOff = new move('O', 0, 0);
            public static move toolOn = new move('I', 0, 0);

        }
    }

    public static charGcode getGcode(char c, double cursor_x, double cursor_y) {
        gchar g = null;
        for (gchar gc : chars) {
            if (gc.unicode.equals(c + "")) {
                g = gc;
                break;
            }
        }
        if (g == null) {
            error("Char not found: " + c);
            return new charGcode(0, new ArrayList<>());
        }

        ArrayList<String> res = new ArrayList<>();
        res.addAll(g.toString(cursor_x, cursor_y, scale));
        if (res.size() > 0) {
            res.add(0, " ;starting char " + c);
            // res.set(0, res.get(0) + " ;starting char " + c);
        }
        return new charGcode(g.x_adv, res);
    }

    record charGcode(double advance, ArrayList<String> gcode) {
    }
}
