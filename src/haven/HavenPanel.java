/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.error.ErrorHandler;

import javax.media.opengl.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings({"SynchronizeOnNonFinalField"})
public class HavenPanel extends GLCanvas implements Runnable {
    UI ui;
    boolean inited = false, rdr = false;
    final long fd = 20;
    long fps = 0;
    int dth = 0, dtm = 0;
    public static int texhit = 0, texmiss = 0;
    final Queue<InputEvent> events = new ConcurrentLinkedQueue<InputEvent>();
    private CursorMode cursmode = CursorMode.TEXTURE;
    private Resource lastcursor = null;
    public Coord mousepos = new Coord(0, 0);
    public final Profile prof = new Profile(300);
    private Profile.Frame curf = null;
    private SyncFSM fsm = null;
    private static final GLCapabilities caps;
    private long last_tick;

    private static enum CursorMode {
        TEXTURE,
        AWT
    }

    static {
        caps = new GLCapabilities();
        caps.setDoubleBuffered(true);
        caps.setAlphaBits(8);
        caps.setRedBits(8);
        caps.setGreenBits(8);
        caps.setBlueBits(8);
    }

    public HavenPanel(Dimension size) {
        super(caps);
        last_tick = System.currentTimeMillis();
        setSize(size);
        initgl();
        if (Toolkit.getDefaultToolkit().getMaximumCursorColors() >= 256) {
            cursmode = CursorMode.AWT;
        }
        setCursor(Toolkit.getDefaultToolkit().createCustomCursor(TexI.mkbuf(new Coord(1, 1)), new java.awt.Point(), ""));
    }

    /**
     * OpenGL Initializer start
     */
    private void initgl() {
        final Thread caller = Thread.currentThread();
        addGLEventListener(new GLEventListener() {

            public void display(GLAutoDrawable d) {
                GL gl = d.getGL();
                if (inited && rdr)
                    redraw(gl);
                TexGL.disposeall(gl);
            }

            public void init(GLAutoDrawable d) {
                GL gl = d.getGL();
                if (caller.getThreadGroup() instanceof ErrorHandler) {
                    ErrorHandler h = (ErrorHandler) caller.getThreadGroup();
                    h.lsetprop("gl.vendor", gl.glGetString(GL.GL_VENDOR));
                    h.lsetprop("gl.version", gl.glGetString(GL.GL_VERSION));
                    h.lsetprop("gl.renderer", gl.glGetString(GL.GL_RENDERER));
                    h.lsetprop("gl.exts", Arrays.asList(Utils.whitespacePattern.split(gl.glGetString(GL.GL_EXTENSIONS))));
                    h.lsetprop("gl.caps", d.getChosenGLCapabilities().toString());
                }
                gl.glColor3f(1, 1, 1);
                gl.glPointSize(4);
                gl.setSwapInterval(1);
                gl.glEnable(GL.GL_BLEND);
                //gl.glEnable(GL.GL_LINE_SMOOTH);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                GOut.checkerr(gl);
            }

            public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
            }

            public void displayChanged(GLAutoDrawable d, boolean cp1, boolean cp2) {
            }
        });
    }

    public void init() {
        setFocusTraversalKeysEnabled(false);
        ui = new UI(getSize(), null);
        addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                checkfs();
                events.add(e);
            }

            public void keyPressed(KeyEvent e) {
                checkfs();
                events.add(e);
            }

            public void keyReleased(KeyEvent e) {
                checkfs();
                events.add(e);
            }
        });
        MouseAdapter mouseListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                checkfs();
                events.add(e);
            }

            public void mouseReleased(MouseEvent e) {
                checkfs();
                events.add(e);
            }

            public void mouseDragged(MouseEvent e) {
                checkfs();
                events.add(e);
            }

            public void mouseMoved(MouseEvent e) {
                checkfs();
                events.add(e);
            }

            public void mouseWheelMoved(MouseWheelEvent e) {
                checkfs();
                events.add(e);
            }
        };
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
        addMouseWheelListener(mouseListener);
        inited = true;
    }

    private class SyncFSM implements FSMan {
        private final FSMan wrapped;
        private boolean tgt;

        private SyncFSM(FSMan wrapped) {
            this.wrapped = wrapped;
            tgt = wrapped.hasfs();
        }

        public void setfs() {
            tgt = true;
        }

        public void setwnd() {
            tgt = false;
        }

        public boolean hasfs() {
            return (tgt);
        }

        private void check() {
            synchronized (ui) {
                if (tgt && !wrapped.hasfs())
                    wrapped.setfs();
                if (!tgt && wrapped.hasfs())
                    wrapped.setwnd();
            }
        }
    }

    private void checkfs() {
        if (fsm != null) {
            fsm.check();
        }
    }

    public void setfsm(FSMan fsm) {
        this.fsm = new SyncFSM(fsm);
        ui.fsm = this.fsm;
    }

    UI newui(Session sess) {
        ui = new UI(getSize(), sess);
        ui.root.gprof = prof;
        ui.fsm = this.fsm;
        return (ui);
    }

    private static Cursor makeawtcurs(BufferedImage img, Coord hs) {
        java.awt.Dimension cd = Toolkit.getDefaultToolkit().getBestCursorSize(img.getWidth(), img.getHeight());
        BufferedImage buf = TexI.mkbuf(new Coord((int) cd.getWidth(), (int) cd.getHeight()));
        java.awt.Graphics g = buf.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return (Toolkit.getDefaultToolkit().createCustomCursor(buf, new java.awt.Point(hs.x, hs.y), ""));
    }

    void redraw(GL gl) {

        long dt = System.currentTimeMillis() - last_tick;
        last_tick = System.currentTimeMillis();
        ui.update(dt);

        GOut g = new GOut(gl, getContext(), CustomConfig.getWindowSize().clone());
        if (Config.render_enable) {

            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrtho(0, getWidth(), 0, getHeight(), -1, 1);
            TexRT.renderall(g);
            if (curf != null)
                curf.tick("texrt");

            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrtho(0, getWidth(), getHeight(), 0, -1, 1);
            gl.glClearColor(0, 0, 0, 1);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            if (curf != null)
                curf.tick("cls");
            synchronized (ui) {
                ui.draw(g);
            }
            if (curf != null)
                curf.tick("draw");

            if (Config.dbtext) {
                g.atext("FPS: " + fps, new Coord(10, 545), 0, 1);
                g.atext("Texhit: " + dth, new Coord(10, 530), 0, 1);
                g.atext("Texmiss: " + dtm, new Coord(10, 515), 0, 1);
                Runtime rt = Runtime.getRuntime();
                long free = rt.freeMemory(), total = rt.totalMemory();
                g.atext(String.format("Mem: %,011d/%,011d/%,011d/%,011d", free, total - free, total, rt.maxMemory()), new Coord(10, 500), 0, 1);
                g.atext(String.format("LCache: %d/%d", Layered.cache.size(), Layered.cache.cached()), new Coord(10, 485), 0, 1);
                g.atext(String.format("RT-current: %d", TexRT.current.get(gl).size()), new Coord(10, 470), 0, 1);
                if (Resource.qdepth() > 0)
                    g.atext(String.format("RQ depth: %d (%d)", Resource.qdepth(), Resource.numloaded()), new Coord(10, 455), 0, 1);
            }
            Object tooltip = ui.root.tooltip(mousepos, true);
            Tex tt = null;
            if (tooltip != null) {
                if (tooltip instanceof Text) {
                    tt = ((Text) tooltip).tex();
                } else if (tooltip instanceof Tex) {
                    tt = (Tex) tooltip;
                } else if (tooltip instanceof String) {
                    if (((String) tooltip).length() > 0)
                        tt = Text.render((String) tooltip).tex();
                }
            }
            if (tt != null) {
                Coord sz = tt.sz();
                Coord pos = mousepos.sub(sz);
                if (pos.x < 0)
                    pos.setX(0);
                if (pos.y < 0)
                    pos.setY(0);
                g.chcolor(244, 247, 21, 192);
                g.rect(pos.add(-3, -3), sz.add(6, 6));
                g.chcolor(35, 35, 35, 192);
                g.frect(pos.add(-2, -2), sz.add(4, 4));
                g.chcolor();
                g.image(tt, pos);
            }
        }
        Resource curs = ui.root.getcurs(mousepos);
        if (!curs.loading.get()) {
            switch (cursmode) {
                case AWT:
                    if (curs == lastcursor) break;
                    try {
                        setCursor(makeawtcurs(curs.layer(Resource.imgc).img, curs.layer(Resource.negc).cc));
                        UI.setCursorName(curs.name);
                        lastcursor = curs;
                    } catch (Exception e) {
                        cursmode = CursorMode.TEXTURE;
                    }
                    break;
                case TEXTURE:
                    Coord dc = mousepos.sub(curs.layer(Resource.negc).cc);
                    g.image(curs.layer(Resource.imgc), dc);
                    break;
            }
        }
    }

    void dispatch() {
        InputEvent e;
        while ((e = events.poll()) != null) {
            if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) e;
                Coord mouseCoord = new Coord(me.getX(), me.getY());
                switch (me.getID()) {
                    case MouseEvent.MOUSE_PRESSED:
                        ui.mousedown(me, mouseCoord, me.getButton());
                        break;
                    case MouseEvent.MOUSE_RELEASED:
                        ui.mouseup(me, mouseCoord, me.getButton());
                        break;
                    case MouseEvent.MOUSE_MOVED:
                    case MouseEvent.MOUSE_DRAGGED:
                        mousepos = mouseCoord;
                        ui.mousemove(me, mousepos);
                        break;
                    case MouseEvent.MOUSE_WHEEL:
                        ui.mousewheel(me, mouseCoord, ((MouseWheelEvent) me).getWheelRotation());
                        break;
                }
            } else if (e instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) e;
                switch (ke.getID()) {
                    case KeyEvent.KEY_PRESSED:
                        ui.keydown(ke);
                        break;
                    case KeyEvent.KEY_RELEASED:
                        ui.keyup(ke);
                        break;
                    case KeyEvent.KEY_TYPED:
                        ui.type(ke);
                        break;
                }
            }
            ui.lastevent = System.currentTimeMillis();
        }
    }

    public void uglyjoglhack() throws InterruptedException {
        try {
            rdr = true;
            display();
        } catch (GLException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw ((InterruptedException) e.getCause());
            } else {
                e.printStackTrace();
                throw (e);
            }
        } finally {
            rdr = false;
        }
    }

    public void run() {
        try {
            long now, fthen, then;
            int frames = 0;
            fthen = System.currentTimeMillis();
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    then = System.currentTimeMillis();
                    if (Config.profile)
                        curf = prof.new Frame();
                    synchronized (ui) {
                        if (ui.sess != null)
                            ui.sess.glob.oc.ctick();
                        dispatch();
                    }
                    if (curf != null)
                        curf.tick("dsp");
                    uglyjoglhack();
                    if (curf != null)
                        curf.tick("aux");
                    frames++;
                    now = System.currentTimeMillis();
                    if (now - then < fd) {
                        synchronized (this) {
                            wait(fd - (now - then));
                        }
                    }
                    if (curf != null)
                        curf.tick("wait");
                    if (now - fthen > 1000) {
                        fps = frames;
                        frames = 0;
                        dth = texhit;
                        dtm = texmiss;
                        texhit = texmiss = 0;
                        fthen = now;
                    }
                    if (curf != null)
                        curf.fin();
                    if (Thread.interrupted())
                        throw (new InterruptedException());
                } catch (NullPointerException e) {
                    System.err.println("NPE in main cycle");
                    e.printStackTrace(System.err);
                }
            }
        } catch (InterruptedException ignored) {
        }
    }

    public GraphicsConfiguration getconf() {
        return (getGraphicsConfiguration());
    }
}
