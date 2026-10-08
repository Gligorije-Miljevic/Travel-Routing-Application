package com.example.projekat.algorithm;

import com.example.projekat.country.City;
import com.example.projekat.country.Country;
import com.example.projekat.country.Station;

import java.time.LocalTime;
import java.util.*;

/**
 * Dijkstra-varijanta koja minimizuje ukupno vrijeme putovanja
 * (voznja + cekanje izmedju veza). Rasporedi polazaka se tretiraju
 * kao periodicni (svakih 24h).
 * <p>
 * Parametar {@code now} je zadrzan radi kompatibilnosti, ali logika
 * koristi apsolutne minute u danu za uporedjivanje veza.
 * </p>
 * @author Gligorije
 */
public class Dijkstra {
    private static final int DAY = 24 * 60;

    /**
     * Wrapper bez zabrana dionica.
     *
     * @param c   drzava
     * @param s   polazni grad
     * @param e   odredisni grad
     * @param now trenutno lokalno vrijeme (nije presudno za algoritam)
     * @return najbrza ruta kao lista dionica; prazno ako nema puta
     */
    public List<Station> nadjiNajkracuRutu(Country c, City s, City e, LocalTime now) {
        return nadjiNajkracuRutu(c, s, e, now, Collections.emptySet());
    }

    /**
     * Najbrza ruta uz mogucnost zabrane pojedinih dionica.
     *
     * @param country drzava
     * @param start   polazni grad
     * @param end     odredisni grad
     * @param now     trenutni sat (zadrzan radi potpisa)
     * @param banned  skup dionica koje treba ignorisati
     * @return lista dionica najkrace rute; prazno ako nema puta
     */
    public List<Station> nadjiNajkracuRutu(
            Country country, City start, City end, LocalTime now, Set<Station> banned) {

        List<Station> all = country.getAllStations();

        Map<String, String> stationToCity = new HashMap<>();
        Map<String, List<Station>> depsByCity = new HashMap<>();
        Map<String, int[]> depBaseMins = new HashMap<>();

        for (City c : country.cities) {
            stationToCity.put(c.busStationName, c.name);
            stationToCity.put(c.trainStationName, c.name);
            depsByCity.put(c.name, new ArrayList<>());
        }
        for (Station s : all) {
            if (banned != null && banned.contains(s)) continue;
            String city = stationToCity.get(s.imePocetneStanice);
            if (city != null) depsByCity.get(city).add(s);
        }
        for (Map.Entry<String, List<Station>> e2 : depsByCity.entrySet()) {
            List<Station> L = e2.getValue();
            L.sort(Comparator.comparing(st -> st.vrijemePolaska));
            int[] mins = new int[L.size()];
            for (int i = 0; i < L.size(); i++) mins[i] = minutes(L.get(i).vrijemePolaska);
            depBaseMins.put(e2.getKey(), mins);
        }

        String startCity = start.name;
        String endBus = end.busStationName, endTrain = end.trainStationName;

        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingLong(st -> st.totalMinutes));
        Map<Station, Long> bestCost = new HashMap<>();
        Map<Station, Station> parent = new HashMap<>();

        for (Station s0 : depsByCity.getOrDefault(startCity, Collections.emptyList())) {
            long travel = diffMinutes(s0.vrijemePolaska, s0.vrijemeDolaska);
            long depAbs = minutes(s0.vrijemePolaska);
            long arrAbs = depAbs + travel;
            State st = new State(s0, travel, arrAbs);
            pq.add(st);
            bestCost.put(s0, travel);
            parent.put(s0, null);
        }

        Station goal = null;

        while (!pq.isEmpty()) {
            State cur = pq.poll();
            Station u = cur.leg;

            Long seenCost = bestCost.get(u);
            if (seenCost != null && cur.totalMinutes > seenCost) continue;

            if (u.imeKrajnjeStanice.equals(endBus) || u.imeKrajnjeStanice.equals(endTrain)) {
                goal = u; break;
            }

            String city = stationToCity.get(u.imeKrajnjeStanice);
            if (city == null) continue;

            List<Station> nexts = depsByCity.getOrDefault(city, Collections.emptyList());
            int[] bases = depBaseMins.getOrDefault(city, new int[0]);
            if (nexts.isEmpty()) continue;

            long earliestBoardAbs;
            int key = (int) (cur.arrivalAbs % DAY);
            int startIdx = lowerBound(bases, key);

            for (int i = startIdx; i < nexts.size(); i++) {
                Station v = nexts.get(i);
                earliestBoardAbs = cur.arrivalAbs + v.minVrijemeCekanja;
                long vDepAbs = roundUpFromBase(bases[i], earliestBoardAbs);
                if (vDepAbs - earliestBoardAbs >= DAY) break;
                relaxEdge(pq, bestCost, parent, cur, v, vDepAbs);
            }
            for (int i = 0; i < startIdx; i++) {
                Station v = nexts.get(i);
                earliestBoardAbs = cur.arrivalAbs + v.minVrijemeCekanja;
                long vDepAbs = roundUpFromBase(bases[i], earliestBoardAbs);
                if (vDepAbs - earliestBoardAbs >= DAY) continue;
                relaxEdge(pq, bestCost, parent, cur, v, vDepAbs);
            }
        }

        if (goal == null) return Collections.emptyList();

        LinkedList<Station> path = new LinkedList<>();
        for (Station x = goal; x != null; x = parent.get(x)) path.addFirst(x);
        return path;
    }

    /** Stavka u PQ: dionica, akumulirano vrijeme i apsolutni dolazak. */
    private static class State {
        Station leg;
        long totalMinutes;
        long arrivalAbs;
        State(Station leg, long totalMinutes, long arrivalAbs) {
            this.leg = leg; this.totalMinutes = totalMinutes; this.arrivalAbs = arrivalAbs;
        }
    }

    /**
     * Relax korak: racuna novo vrijeme i eventualno azurira bestCost/parent.
     *
     * @param pq       prioritetni red
     * @param bestCost najbolji poznati trosak do dionice
     * @param parent   roditeljske veze za rekonstrukciju puta
     * @param cur      trenutno stanje
     * @param v        kandidat sljedece dionice
     * @param vDepAbs  apsolutni minut polaska kandidata
     */
    private static void relaxEdge(PriorityQueue<State> pq,
                                  Map<Station, Long> bestCost,
                                  Map<Station, Station> parent,
                                  State cur, Station v, long vDepAbs) {
        long wait = vDepAbs - cur.arrivalAbs;
        long travel = diffMinutes(v.vrijemePolaska, v.vrijemeDolaska);
        long newArrivalAbs = vDepAbs + travel;
        long newCost = cur.totalMinutes + wait + travel;

        Long bestV = bestCost.get(v);
        if (bestV == null || newCost < bestV) {
            bestCost.put(v, newCost);
            parent.put(v, cur.leg);
            pq.add(new State(v, newCost, newArrivalAbs));
        }
    }

    /** Pretvara LocalTime u minute od pocetka dana. */
    private static int minutes(LocalTime t) { return t.getHour()*60 + t.getMinute(); }

    /**
     * Trajanje izmedju dva vremena u minutama (dozvoljen prelaz preko ponoci).
     *
     * @param t1 polazak
     * @param t2 dolazak
     * @return broj minuta izmedju t1 i t2
     */
    private static long diffMinutes(LocalTime t1, LocalTime t2) {
        int m1 = minutes(t1), m2 = minutes(t2);
        if (m2 < m1) m2 += DAY;
        return m2 - m1;
    }

    /**
     * Za bazni minut polaska vrati prvi polazak koji nije prije {@code earliestAbs}.
     *
     * @param base        bazni minut polaska (0..1439)
     * @param earliestAbs najraniji dozvoljeni apsolutni minut polaska
     * @return apsolutni minut polaska (>= earliestAbs), uz period 24h
     */
    private static long roundUpFromBase(int base, long earliestAbs) {
        if (base >= earliestAbs) return base;
        long d = (earliestAbs - base + DAY - 1) / DAY;
        return base + d * DAY;
    }

    /**
     * Prvi indeks u sortiranom nizu {@code a} koji ima vrijednost >= {@code key}.
     *
     * @param a   sortiran niz
     * @param key trazena vrijednost
     * @return indeks lower-bound pozicije
     */
    private static int lowerBound(int[] a, int key) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] < key) lo = mid + 1; else hi = mid;
        }
        return lo;
    }
}
