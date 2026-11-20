import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.io.*;
import java.util.*;

public class Drawer extends JFrame {

    static final String SAVE_FILE = "graphs.txt";
    static final String EXPORT_FILE = "graphs_H_edges.txt";

    static class Vertex {
        int id, x, y;
        Vertex(int id, int x, int y) { this.id = id; this.x = x; this.y = y; }
    }

    static class Edge {
        int u, v;
        Edge(int a, int b) { if(a<b){u=a;v=b;} else {u=b;v=a;} }
        @Override public boolean equals(Object o) { return o instanceof Edge && ((Edge)o).u==u && ((Edge)o).v==v; }
        @Override public int hashCode() { return Objects.hash(u,v); }
    }

    static class Page {
        String name;
        java.util.List<Vertex> vertices = new ArrayList<>();
        Set<Edge> edges = new HashSet<>();
        Page(String name) { this.name = name; }
    }

    java.util.List<Page> pages = new ArrayList<>();
    int currentPage = -1;

    JPanel pagePanel = new JPanel();
    DrawingPanel drawingPanel = new DrawingPanel();

    public Drawer() {
        super("Graph Drawer");
        setLayout(new BorderLayout());

        pagePanel.setLayout(new BoxLayout(pagePanel, BoxLayout.Y_AXIS));
        JScrollPane pageScrollWheel = new JScrollPane(pagePanel);
        pageScrollWheel.setPreferredSize(new Dimension(200,0));

        JButton newPageButton = new JButton("New Page");
        newPageButton.addActionListener(e -> addPage());

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(newPageButton, BorderLayout.NORTH);
        leftPanel.add(pageScrollWheel, BorderLayout.CENTER);

        JButton helpButton = new JButton("Help");
        helpButton.addActionListener(e -> openHelpPopup());

        add(leftPanel, BorderLayout.WEST);
        add(drawingPanel, BorderLayout.CENTER);
        add(helpButton, BorderLayout.SOUTH);

        setupKeyBindings();
        loadFromFile();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000,700);
        setVisible(true);
    }

    void setupKeyBindings() {
        InputMap im = drawingPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = drawingPanel.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),"save");
        am.put("save",new AbstractAction() {
            public void actionPerformed(ActionEvent e) { saveFiles(); }
        });
    }

    void addPage() {
        String name = JOptionPane.showInputDialog(this,"Page name:");
        if(name==null || name.isEmpty()) return;
        pages.add(new Page(name));
        currentPage = pages.size()-1;
        refreshPageList();
        repaint();
    }

    void openHelpPopup() {
        String helpText =
                "<html><body style='width: 320px; font-family: sans-serif;'>" +
                        "<h3>Graph Drawer Controls</h3>" +
                        "<b>Left Sidebar</b>" +
                        "<ul>" +
                        "<li><b>New Page:</b> Creates a new graph page.</li>" +
                        "<li><b>Switch Page:</b> Click on the pages in the sidebar</li>" +
                        "<li><b>Delete Page:</b> Click on red button next to the graph page.</li>" +
                        "</ul>" +
                        "<b>Main Screen</b>" +
                        "<ul>" +
                        "<li><b>Create vertex:</b> Left-click on the screen.</li>" +
                        "<li><b>Create an edge:</b> Left-click on Vertex and drag to another Vertex.</li>" +
                        "<li><b>Delete vertex:</b> Right-click on the vertex.</li>" +
                        "<li><b>Delete an edge:</b> Right-click on the edge.</li>" +
                        "</ul>" +
                        "<b>Saving/Exporting</b>" +
                        "<ul>" +
                        "<li><b>Ctrl + S:</b> Saves the page layout to <code>graphs.txt</code> and exports the edge list to <code>graphs_H_edges.txt</code></li>" +
                        "</ul>" +
                        "</body></html>";

        JOptionPane.showMessageDialog(this, helpText, "Graph Drawer Help", JOptionPane.INFORMATION_MESSAGE
        );
    }

    void refreshPageList() {
        pagePanel.removeAll();
        for(int i=0;i<pages.size();i++) {
            int idx=i;
            Page p = pages.get(i);

            JPanel row = new JPanel(new BorderLayout());
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));

            JButton select = new JButton(p.name);
            select.addActionListener(e -> { currentPage=idx; repaint(); });

            JButton del = new JButton();
            del.setBackground(Color.RED);
            del.setPreferredSize(new Dimension(20,20));
            del.addActionListener(e -> {
                pages.remove(idx);
                currentPage = pages.isEmpty()?-1:0;
                refreshPageList();
                repaint();
            });

            row.add(select,BorderLayout.CENTER);
            row.add(del,BorderLayout.EAST);
            pagePanel.add(row);
        }
        pagePanel.revalidate();
        pagePanel.repaint();
    }

    class DrawingPanel extends JPanel {
        Vertex dragStart = null;
        final int R = 12;

        DrawingPanel() {
            setBackground(Color.WHITE);

            MouseAdapter mouse = new MouseAdapter() {

                public void mousePressed(MouseEvent e) {
                    if(currentPage<0) return;
                    Page p = pages.get(currentPage);

                    if(SwingUtilities.isRightMouseButton(e)) {
                        Vertex v = findVertex(p,e.getX(),e.getY());
                        if(v!=null) { deleteVertex(p,v); repaint(); return; }
                        Edge ed = findEdge(p,e.getX(),e.getY());
                        if(ed!=null) { p.edges.remove(ed); repaint(); return; }
                        return;
                    }

                    dragStart = findVertex(p,e.getX(),e.getY());
                    if(dragStart==null) {
                        p.vertices.add(new Vertex(p.vertices.size(),e.getX(),e.getY()));
                        repaint();
                    }
                }

                public void mouseReleased(MouseEvent e) {
                    if(dragStart!=null) {
                        Page p = pages.get(currentPage);
                        Vertex end = findVertex(p,e.getX(),e.getY());
                        if(end!=null && end!=dragStart)
                            p.edges.add(new Edge(dragStart.id,end.id));
                    }
                    dragStart=null;
                    repaint();
                }
            };

            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        Vertex findVertex(Page p,int x,int y) {
            for(Vertex v:p.vertices)
                if(Math.hypot(v.x-x,v.y-y)<=R) return v;
            return null;
        }

        Edge findEdge(Page p,int x,int y) {
            for(Edge e:p.edges){
                Vertex a=p.vertices.get(e.u),b=p.vertices.get(e.v);
                if(Line2D.ptSegDist(a.x,a.y,b.x,b.y,x,y)<6) return e;
            }
            return null;
        }

        void deleteVertex(Page p,Vertex v) {
            int id = v.id;
            p.vertices.remove(id);
            Set<Edge> newEdges = new HashSet<>();
            for(Edge e:p.edges) {
                if(e.u==id || e.v==id) continue;
                int u=e.u>id?e.u-1:e.u;
                int w=e.v>id?e.v-1:e.v;
                newEdges.add(new Edge(u,w));
            }
            p.edges = newEdges;
            for(int i=0;i<p.vertices.size();i++) p.vertices.get(i).id=i;
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(currentPage<0) return;
            Page p = pages.get(currentPage);
            g.setColor(Color.BLACK);
            for(Edge e:p.edges){
                Vertex a=p.vertices.get(e.u),b=p.vertices.get(e.v);
                g.drawLine(a.x,a.y,b.x,b.y);
            }
            for(Vertex v:p.vertices){
                g.setColor(Color.LIGHT_GRAY);
                g.fillOval(v.x-R,v.y-R,2*R,2*R);
                g.setColor(Color.BLACK);
                g.drawOval(v.x-R,v.y-R,2*R,2*R);
                g.drawString(""+v.id,v.x-4,v.y+4);
            }
        }
    }

    void saveFiles() {
        try (PrintWriter ui = new PrintWriter(new FileWriter(SAVE_FILE));
             PrintWriter ex = new PrintWriter(new FileWriter(EXPORT_FILE))) {

            for (Page p : pages) {
                ui.println("#PAGE " + p.name);
                for (Vertex v : p.vertices)
                    ui.println("VERTEX " + v.id + " " + v.x + " " + v.y);
                for (Edge e : p.edges)
                    ui.println("EDGE " + e.u + " " + e.v);
                ui.println();
                ex.println("Page: " + p.name);
                for (Edge e : p.edges)
                    ex.println("H.addEdge(" + e.u + "," + e.v + ");");
                ex.println();
            }

            JOptionPane.showMessageDialog(this,"Saved:\n" + SAVE_FILE + "\n" + EXPORT_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void loadFromFile() {
        File f = new File(SAVE_FILE);
        if(!f.exists()) return;
        try(BufferedReader br = new BufferedReader(new FileReader(f))){
            String line;
            Page current=null;
            while((line=br.readLine())!=null){
                if(line.startsWith("#PAGE")) {
                    current = new Page(line.substring(6).trim());
                    pages.add(current);
                } else if(line.startsWith("VERTEX")) {
                    String[] t=line.split(" ");
                    current.vertices.add(new Vertex(Integer.parseInt(t[1]),Integer.parseInt(t[2]),Integer.parseInt(t[3])));
                } else if(line.startsWith("EDGE")) {
                    String[] t=line.split(" ");
                    current.edges.add(new Edge(Integer.parseInt(t[1]),Integer.parseInt(t[2])));
                }
            }
        } catch(IOException e){ e.printStackTrace(); }
        if(!pages.isEmpty()) currentPage=0;
        refreshPageList();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Drawer::new);
    }
}
