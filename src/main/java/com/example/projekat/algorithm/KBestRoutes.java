package com.example.projekat.algorithm;

import com.example.projekat.country.City;
import com.example.projekat.country.Country;
import com.example.projekat.country.Station;

import java.time.LocalTime;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Generisanje Top-K alternativnih ruta za razlicite kriterijume.
 * <p>
 * Koristi bazni algoritam (Dijkstra/BFS) i strategiju zabranjivanja
 * pojedinih dionica (edge banning) da bi pronasao razlicite rute.
 * </p>
 * @author Gligorije
 */
public class KBestRoutes {
    /**
     * Top-K po trajanju (najbrze).
     *
     * @param c drzava
     * @param s start
     * @param e cilj
     * @param now referentno vrijeme (zadrzano radi potpisa)
     * @param k broj trazenih ruta
     * @return lista ruta, sortirana po brzini i tie-break pravilima
     */
    public static List<List<Station>> topKFastest(Country c, City s, City e, LocalTime now, int k) {
        Dijkstra base = new Dijkstra();
        BiFunction<Set<Station>, List<Station>, Double> score =
                (ban, path) -> path.isEmpty()
                        ? Double.POSITIVE_INFINITY
                        : (double) RouteMetrics.totalRouteDuration(path);
        List<List<Station>> out = topKGeneric(k,
                banned -> base.nadjiNajkracuRutu(c, s, e, now, banned),
                score
        );
        out.sort(RouteMetrics.cmpFastest());
        return out;
    }

    /**
     * Top-K po cijeni (najjeftinije).
     *
     * @param c drzava
     * @param s start
     * @param e cilj
     * @param k broj trazenih ruta
     * @return lista ruta, sortirana po cijeni i tie-break pravilima
     */
    public static List<List<Station>> topKCheapest(Country c, City s, City e, int k) {
        DijkstraCijena base = new DijkstraCijena();
        BiFunction<Set<Station>, List<Station>, Double> score =
                (ban, path) -> path.isEmpty()
                        ? Double.POSITIVE_INFINITY
                        : (double) RouteMetrics.totalPrice(path);
        List<List<Station>> out = topKGeneric(k,
                banned -> base.nadjiNajjeftinijuRutu(c, s, e, banned),
                score
        );
        out.sort(RouteMetrics.cmpCheapest());
        return out;
    }

    /**
     * Top-K po broju presjedanja (najmanje).
     *
     * @param c drzava
     * @param s start
     * @param e cilj
     * @param k broj trazenih ruta
     * @return lista ruta, sortirana po broju presjedanja i tie-break pravilima
     */
    public static List<List<Station>> topKMinTransfers(Country c, City s, City e, int k) {
        BFSMinPresjedanja base = new BFSMinPresjedanja();
        BiFunction<Set<Station>, List<Station>, Double> score =
                (ban, path) -> path.isEmpty()
                        ? Double.POSITIVE_INFINITY
                        : (double) RouteMetrics.transfers(path);
        List<List<Station>> out = topKGeneric(k,
                banned -> base.nadjiRutuMinPresjedanja(c, s, e, banned),
                score
        );
        out.sort(RouteMetrics.cmpMinTransfers());
        return out;
    }

    /** Runner koji pokrece bazni trazilac uz skup zabranjenih dionica. */
    interface Runner { List<Station> run(Set<Station> banned); }

    /**
     * Genericki Top-K na osnovu baznog trazioca i funkcije skoringa.
     * <p>
     * Prva ruta je bazna, a ostale se dobijaju zabranjivanjem pojedinih
     * dionica i ponovnim pokretanjem baznog trazioca.
     *
     * @param k koliko ruta vracati
     * @param run bazni algoritam: prima skup zabranjenih dionica
     * @param scoreFn funkcija koja pretvara rutu u broj manji=bolji
     * @return do K razlicitih ruta
     */
    private static List<List<Station>> topKGeneric(int k, Runner run,
                                                   BiFunction<Set<Station>, List<Station>, Double> scoreFn) {
        List<List<Station>> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        PriorityQueue<Cand> heap = new PriorityQueue<>(Comparator.comparingDouble(c -> c.score));

        List<Station> best = run.run(Collections.emptySet());
        if (best.isEmpty()) return results;
        results.add(best);
        seen.add(signature(best));

        for (Station leg : best) {
            Set<Station> ban = new HashSet<>();
            ban.add(leg);
            List<Station> alt = run.run(ban);
            if (!alt.isEmpty()) {
                String sig = signature(alt);
                if (seen.add(sig)) heap.add(new Cand(alt, ban, scoreFn.apply(ban, alt)));
            }
        }

        while (results.size() < k && !heap.isEmpty()) {
            Cand cur = heap.poll();
            results.add(cur.path);
            for (Station leg : cur.path) {
                if (cur.banned.contains(leg)) continue;
                Set<Station> ban2 = new HashSet<>(cur.banned);
                ban2.add(leg);
                List<Station> alt = run.run(ban2);
                if (alt.isEmpty()) continue;
                String sig = signature(alt);
                if (seen.add(sig)) heap.add(new Cand(alt, ban2, scoreFn.apply(ban2, alt)));
            }
        }
        return results;
    }

    /** Kandidat u PQ za Top-K. */
    private record Cand(List<Station> path, Set<Station> banned, double score) {}

    /**
     * Tekstualni potpis rute za deduplikaciju (ne dozvoljava iste rute).
     *
     * @param path ruta
     * @return potpis rute
     */
    private static String signature(List<Station> path) {
        StringBuilder sb = new StringBuilder();
        for (Station s : path) {
            sb.append(s.imePocetneStanice).append("->").append(s.imeKrajnjeStanice).append("|")
                    .append(s.vrijemePolaska).append("->").append(s.vrijemeDolaska).append(";");
        }
        return sb.toString();
    }
}
