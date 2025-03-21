package haven;

import haven.automated.AutoRepeatFlowerMenuScript;

import java.awt.Color;
import java.sql.*;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Math.PI;

public class FlowerMenu extends Widget {
    public static final Color pink = new Color(255, 0, 128);
    public static final Color ptc = Color.WHITE;
    public static final Text.Foundry ptf = new Text.Foundry(Text.dfont, 12);
    public static final IBox pbox = Window.wbox;
    public static final Tex pbg = Window.bg;
    public static final int ph = UI.scale(30), ppl = 8;
    public Petal[] opts;
    private UI.Grab mg, kg;
    public static final Color ptcRed = new Color(255, 50, 50);
    public static final Color ptcGreen = new Color(0, 200, 50);
    public static final Color ptcYellow = new Color(252, 186, 3);
    public static final Color ptcStroke = Color.BLACK;
    private static String nextAutoSel;
    public final String[] options;
    private static final String DATABASE = "jdbc:sqlite:static_data.db";
    public static Map<String, Boolean> autoSelectMap = new TreeMap<>();
    private final WItem sourceItem;

    @RName("sm")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            String[] opts = new String[args.length];
            for (int i = 0; i < args.length; i++)
                opts[i] = (String) args[i];
            return new FlowerMenu(null, opts);
        }
    }

    public class Petal extends Widget {
        public String name;
        public double ta, tr;
        public int num;
        private Text text;
        private double a = 1;

        public Petal(String name, int i) {
            super(Coord.z);
            i = i + 1;
            this.name = name;
            if (name.equals("Steal")) {
                text = ptf.renderstroked(i + ". " + ">> STEAL <<", ptcRed, ptcStroke);
            } else if (name.equals("Invite")) {
                text = ptf.renderstroked(i + ". " + "Invite to Party", ptcGreen, ptcStroke);
            } else if (name.equals("Memorize")) {
                text = ptf.renderstroked(i + ". " + name, ptcYellow, ptcStroke);
            } else {
                text = ptf.renderstroked(i + ". " + name, ptc, ptcStroke);
            }
            resize(text.sz().x + UI.scale(25), ph);
        }

        public void move(Coord c) { this.c = c.sub(sz.div(2)); }
        public void move(double a, double r) { move(Coord.sc(a, r)); }
        public void draw(GOut g) {
            g.chcolor(new Color(255, 255, 255, (int) (255 * a)));
            g.image(pbg, new Coord(3, 3), new Coord(3, 3), sz.add(new Coord(-6, -6)), UI.scale(pbg.sz()));
            pbox.draw(g, Coord.z, sz);
            g.image(text.tex(), sz.div(2).sub(text.sz().div(2)));
        }
        public boolean mousedown(MouseDownEvent ev) { choose(this); return true; }
        public Area ta(Coord tc) { return Area.sized(tc.sub(sz.div(2)), sz); }
        public Area ta(double a, double r) { return ta(Coord.sc(a, r)); }
    }

    private static double nxf(double a) { return (-1.8633 * a * a + 2.8633 * a); }

    public class Opening extends NormAnim {
        Opening() { super(0.0); }
        public void ntick(double s) {
            double ival = 0.8;
            double off = (opts.length == 1) ? 0.0 : ((1.0 - ival) / (opts.length - 1));
            for (int i = 0; i < opts.length; i++) {
                Petal p = opts[i];
                double a = Utils.clip((s - (off * i)) * (1.0 / ival), 0, 1);
                double b = nxf(a);
                p.move(p.ta + ((1 - b) * PI), p.tr * b);
                p.a = a;
            }
        }
    }

    public class Chosen extends NormAnim {
        Petal chosen;
        Chosen(Petal c) { super(0.0); chosen = c; }
        public void ntick(double s) {
            double ival = 0.8;
            double off = ((1.0 - ival) / (opts.length - 1));
            for (int i = 0; i < opts.length; i++) {
                Petal p = opts[i];
                if (p == chosen) {
                    if (s > 0.6) { p.a = 1 - ((s - 0.6) / 0.4); }
                    else if (s < 0.3) { double a = nxf(s / 0.3); p.move(p.ta, p.tr * (1 - a)); }
                } else {
                    if (s > 0.3) { p.a = 0; }
                    else { double a = Utils.clip((s - (off * i)) * (1.0 / ival), 0, 1); p.a = 1 - a; }
                }
            }
            if (s == 1.0) ui.destroy(FlowerMenu.this);
        }
    }

    public class Cancel extends NormAnim {
        Cancel() { super(0.0); }
        public void ntick(double s) {
            double ival = 0.8;
            double off = (opts.length == 1) ? 0.0 : ((1.0 - ival) / (opts.length - 1));
            for (int i = 0; i < opts.length; i++) {
                Petal p = opts[i];
                double a = Utils.clip((s - (off * i)) * (1.0 / ival), 0, 1);
                double b = 1.0 - nxf(1.0 - a);
                p.move(p.ta + (b * PI), p.tr * (1 - b));
                p.a = 1 - a;
            }
            if (s == 1.0) ui.destroy(FlowerMenu.this);
        }
    }

    private void organize(Petal[] opts) { /* Unchanged */ }
    private void organizeVertical(Petal[] options) { /* Unchanged */ }

    public FlowerMenu(WItem source, String... options) {
        super(Coord.z);
        this.sourceItem = source;
        if (source != null) {
            this.options = Arrays.copyOf(options, options.length + 1);
            this.options[options.length] = "Info";
        } else {
            this.options = options;
        }
        addOptionsToDatabase(this.options);
        opts = new Petal[this.options.length];
        for (int i = 0; i < this.options.length; i++) {
            add(opts[i] = new Petal(this.options[i], i));
            opts[i].num = i;
        }
    }

    protected void added() { /* Unchanged */ }
    public boolean mousedown(MouseDownEvent ev) { /* Unchanged */ }
    public void uimsg(String msg, Object... args) { /* Unchanged */ }
    public void draw(GOut g) { super.draw(g, false); }
    public boolean keydown(KeyDownEvent ev) { /* Unchanged */ }

    public void choose(Petal option) {
        if (option != null) {
            if ("Info".equals(option.name) && sourceItem != null) {
                sourceItem.showWikiInfo(); // Assumes showWikiInfo is public
            } else {
                if (AutoRepeatFlowerMenuScript.option != null) {
                    AutoRepeatFlowerMenuScript.option = option.name;
                }
                wdgmsg("cl", option.num, ui.modflags());
            }
        } else {
            wdgmsg("cl", -1);
        }
    }

    public static void setNextSelection(String name) { nextAutoSel = name; }
    public void tryAutoSelect() { /* Unchanged */ }
    public Petal getPetalFromName(String name) { /* Unchanged */ }
    public static void updateValue(String name, boolean value) { /* Unchanged */ }
    private void addOptionsToDatabase(String[] options) { /* Unchanged */ }

    public static void updateDbValue(String flowerMenuOptionName, boolean newValue) {
        try (java.sql.Connection conn = DriverManager.getConnection(DATABASE)) {
            String updateSql = "UPDATE flower_menu_options SET auto_use = ? WHERE name = ?";
            try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                updatePstmt.setBoolean(1, newValue);
                updatePstmt.setString(2, flowerMenuOptionName);
                updatePstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Problem with updating flower menu option in the database.");
        }
    }

    private static void checkAndInsertFlowerMenuOption(String flowerMenuOptionName) {
        try (java.sql.Connection conn = DriverManager.getConnection(DATABASE)) {
            String checkSql = "SELECT count(*) FROM flower_menu_options WHERE name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setString(1, flowerMenuOptionName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.getInt(1) == 0) {
                    String insertSql = "INSERT INTO flower_menu_options(name) VALUES(?)";
                    try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                        insertPstmt.setString(1, flowerMenuOptionName);
                        insertPstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException ignored) {
            System.out.println("Problem with inserting flower menu option to database.");
        }
    }

    public static void fillAutoChooseMap() { /* Unchanged */ }
    public static void createDatabaseIfNotExist() throws SQLException { /* Unchanged */ }
    private static void createSchemaElementIfNotExist(java.sql.Connection conn, String name, String definitions, String type) throws SQLException { /* Unchanged */ }
    private static boolean schemaElementExists(java.sql.Connection conn, String name, String type) throws SQLException { /* Unchanged */ }
}
