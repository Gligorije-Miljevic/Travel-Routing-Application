package com.example.projekat.algorithm;

import com.example.projekat.country.City;
import com.example.projekat.country.Country;
import com.example.projekat.country.Station;

import java.time.LocalTime;
import java.util.*;

/**
 * BFS koji minimizuje broj presjedanja (broj dionica),
 * a pritom postuje termine polazaka i minimalno vrijeme presjedanja.
 * <p>
 * Algoritam radi po "nivoima" (layerima). Svaki nivo dodaje tacno 1 dionicu.
 * Za dati grad i trenutni apsolutni minut dolaska, gleda se najraniji
 * dostižni polazak ka svakom susjednom gradu, i pamti se najbolji (najraniji dolazak).
 * Rekonstrukcija rute koristi parent pokazivace po nivoima.
 * </p>
 * @author Gligorije
 */
public class BFSMinPresjedanja {
    private static final int DAY = 24 * 60;

    /** Opis "najboljeg" dolaska u grad na odredjenom nivou BFS-a. */
    private static final class CityBest {
        final long arrivalAbs;
        final Station via;
        final String parentCity;
        CityBest(long arrivalAbs, Station via, String parentCity) {
            this.arrivalAbs = arrivalAbs; this.via = via; this.parentCity = parentCity;
        }
    }

    /** Bez zabrana. */
    public List<Station> nadjiRutuMinPresjedanja(Country country, City start, City end) {
        return nadjiRutuMinPresjedanja(country, start, end, Collections.emptySet());
    }

    /**
     * Najmanje presjedanja uz postovanje termina i minimalnog vremena presjedanja.
     */
    public List<Station> nadjiRutuMinPresjedanja(Country country, City start, City end, Set<Station> banned) {
        Map<String, String> stationToCity = new HashMap<>();
        Map<String, List<Station>> depsByCity = new HashMap<>();
        for (City c : country.cities) {
            stationToCity.put(c.busStationName, c.name);
            stationToCity.put(c.trainStationName, c.name);
            depsByCity.put(c.name, new ArrayList<>());
        }
        for (Station s : country.getAllStations()) {
            if (banned != null && banned.contains(s)) continue;
            String fromCity = stationToCity.get(s.imePocetneStanice);
            if (fromCity != null) depsByCity.get(fromCity).add(s);
        }
        Map<String,int[]> baseMins = new HashMap<>();
        for (Map.Entry<String,List<Station>> e : depsByCity.entrySet()) {
            List<Station> L = e.getValue();
            L.sort(Comparator.comparing(st -> st.vrijemePolaska));
            int[] mins = new int[L.size()];
            for (int i = 0; i < L.size(); i++) mins[i] = minutes(L.get(i).vrijemePolaska);
            baseMins.put(e.getKey(), mins);
        }

        String startCity = start.name, endCity = end.name;
        if (startCity.equals(endCity)) return Collections.emptyList();

        List<Map<String, CityBest>> layers = new ArrayList<>();

        Map<String, CityBest> layer0 = new HashMap<>();
        layer0.put(startCity, new CityBest(0L, null, null));
        layers.add(layer0);

        int level = 0;
        while (true) {
            Map<String, CityBest> cur = layers.get(level);
            Map<String, CityBest> next = new HashMap<>();

            for (Map.Entry<String, CityBest> ent : cur.entrySet()) {
                String uCity = ent.getKey();
                CityBest cb = ent.getValue();

                List<Station> nexts = depsByCity.getOrDefault(uCity, Collections.emptyList());
                int[] bases = baseMins.getOrDefault(uCity, new int[0]);
                if (nexts.isEmpty()) continue;

                for (int i = 0; i < nexts.size(); i++) {
                    Station v = nexts.get(i);
                    String vCity = stationToCity.get(v.imeKrajnjeStanice);
                    if (vCity == null) continue;

                    long earliestBoard = cb.arrivalAbs + v.minVrijemeCekanja;
                    long vDepAbs = roundUpFromBase(bases[i], earliestBoard);
                    long travel = diff(minutes(v.vrijemePolaska), minutes(v.vrijemeDolaska));
                    long vArrAbs = vDepAbs + travel;

                    CityBest exist = next.get(vCity);
                    if (exist == null || vArrAbs < exist.arrivalAbs ||
                            (vArrAbs == exist.arrivalAbs && v.cijenaKarte < exist.via.cijenaKarte)) {
                        next.put(vCity, new CityBest(vArrAbs, v, uCity));
                    }
                }
            }

            if (next.isEmpty()) break;

            layers.add(next);
            level++;

            if (next.containsKey(endCity)) {
                LinkedList<Station> path = new LinkedList<>();
                String curCity = endCity;
                for (int L = level; L > 0; L--) {
                    CityBest cb = layers.get(L).get(curCity);
                    if (cb == null || cb.via == null) return Collections.emptyList();
                    path.addFirst(cb.via);
                    curCity = cb.parentCity;
                }
                return path;
            }
        }

        return Collections.emptyList();
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
}
