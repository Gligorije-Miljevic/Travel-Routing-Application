package com.example.projekat.algorithm;

import com.example.projekat.country.*;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.view.Viewer;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

/**
 * Jednostavan prikaz mape gradova pomocu GraphStream-a
 * i isticanje izabrane rute.
 * <ul>
 *   <li>Gradovi su cvorovi (naziv grada kao id)</li>
 *   <li>Susjedni gradovi u matrici su povezani ivicama</li>
 *   <li>Ruta se istice: cvorovi crno, ivice plavo (autobus) / crveno (voz)</li>
 * </ul>
 * @author Gligorije
 */
public class GraphVisualizer {
    private static Viewer lastViewer;
    private static Viewer currentViewer;
    private static Graph  currentGraph;

    /**
     * Kreira graf iz Country (matrica gradova).
     *
     * @param country ulazna struktura
     * @return novi graf (MultiGraph)
     */
    public static Graph buildGraph(Country country) {
        Graph graph = new MultiGraph("Transport");
        graph.setAttribute("ui.stylesheet",
                "node { fill-color: #777; size: 14px; text-size: 14; }" +
                        "edge { fill-color: #bbb; size: 1px; }"
        );
        for (City city : country.cities) {
            Node n = graph.addNode(city.name);
            n.setAttribute("ui.label", city.name);
        }
        for (int i = 0; i < country.SIZE_N; i++) {
            for (int j = 0; j < country.SIZE_M; j++) {
                City cur = country.cities.get(i * country.SIZE_M + j);
                if (i > 0) addEdge(graph, cur, country.cities.get((i - 1) * country.SIZE_M + j));
                if (i < country.SIZE_N - 1) addEdge(graph, cur, country.cities.get((i + 1) * country.SIZE_M + j));
                if (j > 0) addEdge(graph, cur, country.cities.get(i * country.SIZE_M + (j - 1)));
                if (j < country.SIZE_M - 1) addEdge(graph, cur, country.cities.get(i * country.SIZE_M + (j + 1)));
            }
        }
        return graph;
    }

    private static void addEdge(Graph g, City a, City b) { addEdge(g, a, b, ""); }

    /**
     * Dodaje neorijentisanu ivicu izmedju dva grada, ako vec ne postoji.
     *
     * @param g graf
     * @param a prvi grad
     * @param b drugi grad
     * @param type opis (postavlja se kao label)
     */
    private static void addEdge(Graph g, City a, City b, String type) {
        String u = a.name, v = b.name;
        String id = (u.compareTo(v) < 0) ? u + "-" + v : v + "-" + u;
        if (g.getEdge(id) == null) {
            Edge e = g.addEdge(id, u, v);
            e.setAttribute("type", type);
            e.setAttribute("ui.label", type);
        }
    }

    /**
     * Generise ID ivice neovisno o smjeru.
     *
     * @param u ime cvora A
     * @param v ime cvora B
     * @return id kao "min-max"
     */
    private static String edgeId(String u, String v) {
        return (u.compareTo(v) < 0) ? u + "-" + v : v + "-" + u;
    }

    /**
     * Istice putanju na grafu: cvorovi crno, ivice plavo/crveno po tipu dionice.
     *
     * @param g graf
     * @param path lista naziva gradova po redu
     * @param ruta iste dionice radi odabira boje ivica
     */
    public static void highlightPath(Graph g, List<String> path, List<Station> ruta) {
        for (int i = 0; i < path.size(); i++) {
            Node n = g.getNode(path.get(i));
            if (n != null) n.setAttribute("ui.style", "fill-color: black; size: 18px;");
            if (i < path.size() - 1) {
                Edge e = g.getEdge(edgeId(path.get(i), path.get(i + 1)));
                if (e != null && i < ruta.size()) {
                    boolean isBus = ruta.get(i).imePocetneStanice.charAt(0) == 'A';
                    e.setAttribute("ui.style", "fill-color: " + (isBus ? "blue" : "red") + "; size: 3px;");
                }
            }
        }
    }

    /**
     * Gradi listu cvorova (imena gradova) iz rute dionica.
     *
     * @param ruta dionice
     * @param end zavrsni grad (dodaje se kao posljednji cvor)
     * @return lista imena cvorova
     */
    public static List<String> buildPathNodes(List<Station> ruta, City end) {
        List<String> path = new ArrayList<>();
        for (Station s : ruta) path.add(s.city.name);
        if (end != null) path.add(end.name);
        return path;
    }

    /**
     * Otvara prozor grafika u posebnoj niti, prikazuje mapu i oznacava rutu.
     * Stari prikaz (ako postoji) se pokusava uredno zatvoriti.
     *
     * @param country model drzave
     * @param ruta ruta za isticanje
     * @param end zavrsni grad
     */
    public static void renderRouteAsync(Country country, List<Station> ruta, City end) {
        new Thread(() -> {
            if (lastViewer != null) {
                Viewer v = lastViewer;
                lastViewer = null;
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        try { v.disableAutoLayout(); } catch (Exception ignored) {}
                        try { v.close(); } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }
            Graph g = buildGraph(country);
            try {
                SwingUtilities.invokeAndWait(() -> {
                    lastViewer = g.display();
                    lastViewer.enableAutoLayout();
                    lastViewer.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);
                });
            } catch (Exception ignored) {}
            List<String> path = buildPathNodes(ruta, end);
            highlightPath(g, path, ruta);
        }, "GraphStream-Refresh").start();
    }

    /**
     * Zatvara i ociscava aktivni prikaz/graf ako postoje.
     */
    public static synchronized void shutdown() {
        try { if (currentViewer != null) currentViewer.close(); } catch (Exception ignored) {}
        try { if (currentGraph  != null) currentGraph.clear(); } catch (Exception ignored) {}
        currentViewer = null;
        currentGraph  = null;
    }
}
