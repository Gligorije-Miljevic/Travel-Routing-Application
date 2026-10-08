package com.example.projekat.algorithm;

import com.example.projekat.country.City;
import com.example.projekat.country.Country;
import com.example.projekat.country.Station;

import java.time.LocalTime;
import java.util.*;

/**
 * Dijkstra-varijanta koja minimizuje ukupnu cijenu rute
 * uz postovanje rasporeda voznji i minimalnog vremena presjedanja.
 * <p>
 * Stanje u PQ cuva:
 * <ul>
 *     <li>price – do sada akumulirana cijena</li>
 *     <li>minutes – ukupno vrijeme (voznja + cekanja) radi tie-break-a</li>
 *     <li>legs – broj dionica (sekundarni tie-break)</li>
 *     <li>arrivalAbs – apsolutni minut dolaska (za vremensku konzistenciju)</li>
 * </ul>
 * Poredak u PQ: cijena &lt; vrijeme &lt; broj presjedanja.
 * Periodicni polasci su na 24h (svaki dan isti raspored).
 * Podrzan je i skup zabranjenih dionica (ban) radi alternativnih ruta.
 * @author Gligorije
 */
public class DijkstraCijena {
    private static final int DAY = 24 * 60;

    /** Pomocna struktura za metricke vrijednosti stanja. */
    private static final class Cost implements Comparable<Cost> {
        final int price;
        final long minutes;
        final int legs;
        final long arrivalAbs;

        Cost(int price, long minutes, int legs, long arrivalAbs) {
            this.price = price; this.minutes = minutes; this.legs = legs; this.arrivalAbs = arrivalAbs;
        }
        @Override public int compareTo(Cost o) {
            if (price != o.price) return Integer.compare(price, o.price);
            if (minutes != o.minutes) return Long.compare(minutes, o.minutes);
            return Integer.compare(legs, o.legs);
        }
    }

    /** Stavka u PQ: posljednja dionica + cijena/vrijeme/legs. */
    private static final class State {
        final Station leg; final Cost c;
        State(Station leg, Cost c){ this.leg = leg; this.c = c; }
    }

    /** Bez zabrana. */
    public List<Station> nadjiNajjeftinijuRutu(Country c, City s, City e) {
        return nadjiNajjeftinijuRutu(c, s, e, Collections.emptySet());
    }

    /**
     * Najjeftinija realno-izvodiva ruta (po cijeni) uz mogucnost banovanja dionica.
     */
    public List<Station> nadjiNajjeftinijuRutu(Country country, City start, City end, Set<Station> banned) {
        Map<String,String> stationToCity = new HashMap<>();
        Map<String,List<Station>> depsByCity = new HashMap<>();
        for (City c : country.cities) {
            stationToCity.put(c.busStationName, c.name);
            stationToCity.put(c.trainStationName, c.name);
            depsByCity.put(c.name, new ArrayList<>());
        }
        for (Station s : country.getAllStations()) {
            if (banned != null && banned.contains(s)) continue;
            String city = stationToCity.get(s.imePocetneStanice);
            if (city != null) depsByCity.get(city).add(s);
        }
        Map<String,int[]> baseMins = new HashMap<>();
        for (Map.Entry<String,List<Station>> e : depsByCity.entrySet()) {
            List<Station> L = e.getValue();
            L.sort(Comparator.comparing(st -> st.vrijemePolaska));
            int[] mins = new int[L.size()];
            for (int i = 0; i < L.size(); i++) mins[i] = minutes(L.get(i).vrijemePolaska);
            baseMins.put(e.getKey(), mins);
        }

        String startCity = start.name;
        String endBus = end.busStationName, endTrain = end.trainStationName;

        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparing(st -> st.c));
        Map<Station, Cost> best = new HashMap<>();
        Map<Station, Station> parent = new HashMap<>();

        for (Station s0 : depsByCity.getOrDefault(startCity, Collections.emptyList())) {
            long travel = diff(minutes(s0.vrijemePolaska), minutes(s0.vrijemeDolaska));
            long depAbs = minutes(s0.vrijemePolaska);
            long arrAbs = depAbs + travel;
            Cost c0 = new Cost(s0.cijenaKarte, travel, 1, arrAbs);
            State st = new State(s0, c0);
            pq.add(st);
            best.put(s0, c0);
            parent.put(s0, null);
        }

        Station goal = null;

        while (!pq.isEmpty()) {
            State cur = pq.poll();
            Station u = cur.leg;

            Cost seen = best.get(u);
            if (seen != null && seen.compareTo(cur.c) < 0) continue;

            if (u.imeKrajnjeStanice.equals(endBus) || u.imeKrajnjeStanice.equals(endTrain)) {
                goal = u; break;
            }

            String city = stationToCity.get(u.imeKrajnjeStanice);
            if (city == null) continue;

            List<Station> nexts = depsByCity.getOrDefault(city, Collections.emptyList());
            int[] bases = baseMins.getOrDefault(city, new int[0]);
            if (nexts.isEmpty()) continue;

            int key = (int) (cur.c.arrivalAbs % DAY);
            int startIdx = lowerBound(bases, key);

            for (int i = startIdx; i < nexts.size(); i++) {
                Station v = nexts.get(i);
                long earliestBoard = cur.c.arrivalAbs + v.minVrijemeCekanja;
                long vDepAbs = roundUpFromBase(bases[i], earliestBoard);
                long wait = vDepAbs - cur.c.arrivalAbs;
                long travel = diff(minutes(v.vrijemePolaska), minutes(v.vrijemeDolaska));
                Cost nc = new Cost(
                        cur.c.price + v.cijenaKarte,
                        cur.c.minutes + wait + travel,
                        cur.c.legs + 1,
                        vDepAbs + travel
                );
                Cost bestV = best.get(v);
                if (bestV == null || nc.compareTo(bestV) < 0) {
                    best.put(v, nc);
                    parent.put(v, u);
                    pq.add(new State(v, nc));
                }
            }
            for (int i = 0; i < startIdx; i++) {
                Station v = nexts.get(i);
                long earliestBoard = cur.c.arrivalAbs + v.minVrijemeCekanja;
                long vDepAbs = roundUpFromBase(bases[i], earliestBoard);
                long wait = vDepAbs - cur.c.arrivalAbs;
                long travel = diff(minutes(v.vrijemePolaska), minutes(v.vrijemeDolaska));
                Cost nc = new Cost(
                        cur.c.price + v.cijenaKarte,
                        cur.c.minutes + wait + travel,
                        cur.c.legs + 1,
                        vDepAbs + travel
                );
                Cost bestV = best.get(v);
                if (bestV == null || nc.compareTo(bestV) < 0) {
                    best.put(v, nc);
                    parent.put(v, u);
                    pq.add(new State(v, nc));
                }
            }
        }

        if (goal == null) return Collections.emptyList();

        LinkedList<Station> path = new LinkedList<>();
        for (Station x = goal; x != null; x = parent.get(x)) path.addFirst(x);
        return path;
    }

    private static int minutes(LocalTime t){ return t.getHour()*60 + t.getMinute(); }

    private static long diff(int dep, int arr){
        int a = dep, b = arr;
        if (b < a) b += DAY;
        return b - a;
    }

    private static long roundUpFromBase(int base, long earliest){
        if (base >= earliest) return base;
        long d = (earliest - base + DAY - 1) / DAY;
        return base + d * DAY;
    }

    private static int lowerBound(int[] a, int key){
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] < key) lo = mid + 1; else hi = mid;
        }
        return lo;
    }
}
