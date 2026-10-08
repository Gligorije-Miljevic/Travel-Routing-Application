package com.example.projekat.algorithm;

import com.example.projekat.country.Station;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Pomocne metrike i komparatori za poredjenje ruta.
 * <p>
 * Rad sa vremenom uzima u obzir prelaz preko ponoci (24h wrap).
 * </p>
 * @author Gligorije
 */
public final class RouteMetrics {
    private static final int DAY = 24 * 60;
    private RouteMetrics(){}

    /** Pretvara LocalTime u minute od pocetka dana. */
    private static int m(LocalTime t){ return t.getHour()*60 + t.getMinute(); }

    /**
     * Razlika minuta izmedju dva vremena (dozvoljen prelaz preko ponoci).
     *
     * @param a polazno vrijeme
     * @param b dolazno vrijeme
     * @return broj minuta izmedju a i b (>= 0, uz wrap 24h)
     */
    private static int diff(LocalTime a, LocalTime b){
        int x = m(a), y = m(b);
        if (y < x) y += DAY;
        return y - x;
    }

    /**
     * Ukupno vrijeme od trenutka {@code now} do kraja rute
     * (cekanje do prvog polaska + voznje + cekanja izmedju veza).
     *
     * @param path ruta (lista dionica)
     * @param now  trenutno vrijeme
     * @return ukupno trajanje u minutama; vrlo velika vrijednost ako je ruta prazna
     */
    public static long totalMinutesFromNow(List<Station> path, LocalTime now){
        if (path == null || path.isEmpty()) return Long.MAX_VALUE / 4;
        long total = diff(now, path.get(0).vrijemePolaska);
        for (int i = 0; i < path.size(); i++){
            Station s = path.get(i);
            total += diff(s.vrijemePolaska, s.vrijemeDolaska);
            if (i + 1 < path.size()){
                total += diff(s.vrijemeDolaska, path.get(i+1).vrijemePolaska);
            }
        }
        return total;
    }

    /**
     * Ukupno trajanje rute (voznje + cekanja izmedju veza), bez cekanja do prvog polaska.
     *
     * @param path ruta
     * @return ukupno trajanje u minutama
     */
    public static long totalRouteDuration(List<Station> path){
        if (path == null || path.isEmpty()) return 0;
        long total = 0;
        for (int i = 0; i < path.size(); i++){
            Station s = path.get(i);
            total += diff(s.vrijemePolaska, s.vrijemeDolaska);
            if (i + 1 < path.size()){
                total += diff(s.vrijemeDolaska, path.get(i+1).vrijemePolaska);
            }
        }
        return total;
    }

    /**
     * Zbir cijena svih dionica.
     *
     * @param path ruta
     * @return ukupna cijena
     */
    public static int totalPrice(List<Station> path){
        int p = 0;
        if (path != null) for (Station s : path) p += s.cijenaKarte;
        return p;
    }

    /**
     * Broj presjedanja na ruti (broj dionica minus 1).
     *
     * @param path ruta
     * @return broj presjedanja (nikad negativan)
     */
    public static int transfers(List<Station> path){
        return Math.max(0, (path == null ? 0 : path.size()) - 1);
    }

    /**
     * Komparator za kriterijum: najbrze, pa jeftinije, pa manje presjedanja.
     *
     * @return komparator koji poredi rute po trajanju, zatim cijeni, pa presjedanjima
     */
    public static Comparator<List<Station>> cmpFastest(){
        return Comparator
                .comparingLong(RouteMetrics::totalRouteDuration)
                .thenComparingInt(RouteMetrics::totalPrice)
                .thenComparingInt(RouteMetrics::transfers);
    }

    /**
     * Komparator za kriterijum: najjeftinije, pa brze, pa manje presjedanja.
     *
     * @return komparator koji poredi rute po cijeni, zatim trajanju, pa presjedanjima
     */
    public static Comparator<List<Station>> cmpCheapest(){
        return Comparator
                .comparingInt(RouteMetrics::totalPrice)
                .thenComparingLong(RouteMetrics::totalRouteDuration)
                .thenComparingInt(RouteMetrics::transfers);
    }

    /**
     * Komparator za kriterijum: najmanje presjedanja, pa brze, pa jeftinije.
     *
     * @return komparator koji poredi rute po presjedanjima, zatim trajanju, pa cijeni
     */
    public static Comparator<List<Station>> cmpMinTransfers(){
        return Comparator
                .comparingInt(RouteMetrics::transfers)
                .thenComparingLong(RouteMetrics::totalRouteDuration)
                .thenComparingInt(RouteMetrics::totalPrice);
    }
}
