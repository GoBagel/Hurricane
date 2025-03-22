package haven;

import java.util.*;
import java.util.function.Consumer; // Added import for Consumer
import java.awt.Color;
import java.awt.image.BufferedImage;

import haven.automated.AutoRepeatFlowerMenuScript;

public class WItem extends Widget implements DTarget {
    public static final Resource.Named missing = Resource.local().load("gfx/invobjs/missing");
    public final GItem item;
    private Resource curs;
    private List<ItemInfo> info = Collections.emptyList();
    private final Widget contents;

    public WItem(GItem item) {
        super(item.spr() != null ? item.spr().sz() : Inventory.sqsz);
        this.item = item;
        contents = initcont();
    }

    private Widget initcont() {
        if (item.contents == null)
            return null;
        return add(new ItemContents(this, item.contents));
    }

    public class ItemContents extends Widget {
        public final Widget inv;
        private final List<WItem> items = new ArrayList<>();
        private final Consumer<WItem> added;

        public ItemContents(WItem cont, Widget inv) {
            super(Coord.z);
            this.inv = inv;
            this.added = cont::additem;
            add(inv, 0, 0);
            GItem.ContentsWindow wnd = item.contentswnd;
            if (wnd != null) {
                wnd.hide();
            }
        }

        private void reset() {
            items.clear();
            for (Widget ch = inv.child; ch != null; ch = ch.next) {
                if (ch instanceof WItem)
                    items.add((WItem) ch);
            }
            resize(inv.sz);
        }

        public void addchild(Widget child, Object... args) {
            inv.addchild(child, args);
            if (child instanceof WItem) {
                WItem itm = (WItem) child;
                items.add(itm);
                added.accept(itm);
            }
            resize(inv.sz);
        }

        public void cdestroy(Widget ch) {
            inv.cdestroy(ch);
            if (ch instanceof WItem)
                items.remove(ch);
            resize(inv.sz);
        }

        public void tick(double dt) {
            super.tick(dt);
            if (contents != null)
                contents.tick(dt);
        }
    }

    public void additem(WItem itm) {
        synchronized (itm) {
            itm.info = null;
        }
    }

    public class CustomFlowerMenu extends FlowerMenu {
        private final WItem parentItem;

        public CustomFlowerMenu(WItem parent, String... options) {
            super(parent, options);
            this.parentItem = parent;
            Petal[] newOpts = Arrays.copyOf(opts, opts.length + 1);
            newOpts[opts.length] = new Petal("Info", opts.length);
            opts = newOpts;
        }

        @Override
        public void choose(Petal opt) {
            if (opt != null) {
                if ("Info".equals(opt.name)) {
                    parentItem.showWikiInfo();
                    destroy();
                } else {
                    super.choose(opt);
                }
            } else {
                destroy();
            }
        }
    }

    public void tick(double dt) {
        super.tick(dt);
        if (item.contents != null) {
            if (contents == null) {
                add(new ItemContents(this, item.contents));
            }
        } else {
            if (contents != null) {
                contents.destroy();
            }
        }
    }

    private static final Resource.Named qcursor = Resource.local().load("ui/qcursor");
    public void drawmain(GOut g, GSprite spr) {
        spr.draw(g);
        QBuff qual = item.getQBuff();
        if (qual != null) {
            if (qual.q >= 0) {
                Tex tex = qual.qtex;
                g.image(tex, Coord.z);
            }
            curs = qcursor;
        } else {
            curs = null;
        }
    }

    public void draw(GOut g) {
        GSprite spr = item.spr();
        if (spr != null) {
            g.defstate();
            drawmain(g, spr);
            g.defstate();
            if (item.num >= 0) {
                g.aimage(new TexI(GItem.NumberInfo.numrenderStroked(item.num, Color.WHITE, true)), sz, 1, 1);
            }
            if (item.meter > 0) {
                double lastMeterUpdate = (System.currentTimeMillis() - item.meterUpdated) / 1000.0;
                double meterFadeoutTime = OptWnd.itemMeterFadeoutTimeSlider.val;
                if (lastMeterUpdate < meterFadeoutTime || meterFadeoutTime == 0) {
                    double a = item.meter / 100.0;
                    if (lastMeterUpdate >= 0 && meterFadeoutTime > 0) {
                        double fade = Math.max(0, (meterFadeoutTime - lastMeterUpdate) / meterFadeoutTime);
                        g.chcolor(255, 255, 255, (int) (255 * fade));
                    }
                    double dx = sz.x * a;
                    g.chcolor(0, 0, 0, 96);
                    g.frect(new Coord(0, sz.y - 3), new Coord((int) dx + 1, 3));
                    g.chcolor(255 - (int) (a * 255), (int) (a * 255), 0, 128);
                    g.frect(new Coord(0, sz.y - 2), new Coord((int) dx, 2));
                    g.chcolor();
                }
            }
            if (item.studytime > 0) {
                double m = item.studytime / (100.0 * 3600);
                g.chcolor(0, 0, 0, 96);
                g.frect(new Coord(0, 0), new Coord((int) (sz.x * m) + 1, 3));
                g.chcolor(255 - (int) (m * 255), (int) (m * 255), 0, 128);
                g.frect(new Coord(0, 1), new Coord((int) (sz.x * m), 2));
                g.chcolor();
            }
        } else {
            g.image(missing.loadwait().layer(Resource.imgc).tex(), Coord.z, sz);
        }
    }

    public boolean mousedown(MouseDownEvent ev) {
        boolean inv = parent instanceof Inventory;
        if (ev.b == 1) {
            if (OptWnd.useImprovedInventoryTransferControlsCheckBox.a && ui.modmeta && !ui.modctrl) {
                if (inv) {
                    wdgmsg("transfer-ordered", item, false);
                    return true;
                }
            }
            if (ui.modshift) {
                int n = ui.modctrl ? -1 : 1;
                item.wdgmsg("transfer", ev.c, n);
            } else if (ui.modctrl) {
                int n = ui.modmeta ? -1 : 1;
                item.wdgmsg("drop", ev.c, n);
            } else {
                item.wdgmsg("take", ev.c);
            }
            return true;
        } else if (ev.b == 3) {
            if (OptWnd.useImprovedInventoryTransferControlsCheckBox.a && ui.modmeta && !ui.modctrl) {
                if (inv) {
                    wdgmsg("transfer-ordered", item, true);
                    return true;
                }
            }
            item.wdgmsg("iact", ev.c, ui.modflags(), this);

            if (ui.modctrl && OptWnd.autoSelect1stFlowerMenuCheckBox.a && !ui.modshift && !ui.modmeta) {
                String itemname = item.getname();
                int option = 0;
                if (itemname.equals("Head of Lettuce")) {
                    option = 1;
                }
                item.wdgmsg("iact", ev.c, ui.modflags());
                ui.rcvr.rcvmsg(ui.lastWidgetID + 1, "cl", option, 0);
            }
            if (ui.modctrl && ui.modshift && OptWnd.autoRepeatFlowerMenuCheckBox.a) {
                if (!(item != null && item.contents != null)) {
                    try {
                        if (ui.gui.autoRepeatFlowerMenuScriptThread == null) {
                            ui.gui.autoRepeatFlowerMenuScriptThread = new Thread(new AutoRepeatFlowerMenuScript(ui.gui, this.item.getres().name), "autoRepeatFlowerMenu");
                            ui.gui.autoRepeatFlowerMenuScriptThread.start();
                        } else {
                            ui.gui.autoRepeatFlowerMenuScriptThread.interrupt();
                            ui.gui.autoRepeatFlowerMenuScriptThread = null;
                            ui.gui.autoRepeatFlowerMenuScriptThread = new Thread(new AutoRepeatFlowerMenuScript(ui.gui, this.item.getres().name), "autoRepeatFlowerMenu");
                            ui.gui.autoRepeatFlowerMenuScriptThread.start();
                        }
                    } catch (Loading ignored) {}
                }
            }
            return true;
        }
        return super.mousedown(ev);
    }

    public void drawsel(GOut g) {
        g.chcolor(255, 255, 0, 128);
        g.frect(Coord.z, sz);
        g.chcolor();
    }

    public boolean drop(WItem target, Coord cc) {
        return false;
    }

    public boolean iteminteract(WItem target, Coord cc) {
        return false;
    }

    public Object tooltip(Coord c, Widget prev) {
        try {
            if (prev != this)
                info = item.info();
            if (!info.isEmpty())
                return info;
        } catch (Loading l) {}
        return null;
    }

    private String formatWikiName(String itemName) {
        StringBuilder wikiName = new StringBuilder();
        for (int i = 0; i < itemName.length(); i++) {
            char c = itemName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                wikiName.append('_');
            }
            wikiName.append(c);
        }
        return wikiName.toString().replace(' ', '_');
    }

    public void showWikiInfo() {
        String itemName = item.getname();
        String wikiName = formatWikiName(itemName);
        String url = "https://ringofbrodgar.com/wiki/" + wikiName;
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            ui.cons.out.println("Error opening wiki: " + e.getMessage());
        }
    }

    public void dispose() {
        GItem.ContentsWindow wnd = item.contentswnd;
        if (wnd != null) {
            wnd.hide();
        }
    }
}
