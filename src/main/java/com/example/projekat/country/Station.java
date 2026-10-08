package com.example.projekat.country;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reprezentuje jedan polazak (dionicu rute) izmedju stanica.
 * <p>
 * Svaka instanca opisuje VOZNJU od konkretne pocetne stanice ({@code imePocetneStanice})
 * do krajnje stanice ({@code imeKrajnjeStanice}) sa vremenom polaska/dolaska,
 * cijenom i minimalnim vremenom cekanja prije ukrcaja na sljedeci polazak.
 * </p>
 * @author Gligorije
 */
public class Station {
    /** Naziv pocetne stanice (npr. {@code A_0_1} ili {@code Z_2_3}). */
    public String imePocetneStanice;
    /** Naziv krajnje stanice (npr. {@code A_0_2} ili {@code Z_2_3}). */
    public String imeKrajnjeStanice;
    /** Vrijeme polaska sa pocetne stanice. */
    public LocalTime vrijemePolaska;
    /** Vrijeme dolaska na krajnju stanicu. */
    public LocalTime vrijemeDolaska;
    /** Grad kojem pripada ova stanica/polazak. */
    public City city;
    /** Cijena karte za ovu dionicu. */
    public int cijenaKarte;
    /** Minimalno vrijeme cekanja (u minutama) prije narednog presjedanja. */
    public int minVrijemeCekanja;
    /**
     * Susjedne dionice koje se mogu voziti nakon ove (tj. polasci iz grada u koji stizemo).
     * Popunjava se prilikom ucitavanja podataka.
     */
    public List<Station> neighbours = new ArrayList<>();

    /**
     * Dodaje susjednu dionicu (moguce sljedece putovanje).
     * @param neighbour sljedeca dionica koja se moze odabrati nakon ove
     */
    public void addNeighbour(Station neighbour) {
        neighbours.add(neighbour);
    }

    /**
     * Kreira novu dionicu (polazak) izmedju dvije stanice.
     *
     * @param city               grad kojem pripada polazak
     * @param imePocetneStanice  oznaka pocetne stanice (npr. {@code A_1_2})
     * @param imeKrajnjeStanice  oznaka krajnje stanice (npr. {@code A_1_3})
     * @param vrijemePolaska     vrijeme polaska
     * @param vrijemeDolaska     vrijeme dolaska
     * @param cijenaKarte        cijena karte za ovu dionicu
     * @param minVrijemeCekanja  minimalno cekanje (minute) prije naredne dionice
     */
    public Station(City city, String imePocetneStanice, String imeKrajnjeStanice,
                   LocalTime vrijemePolaska, LocalTime vrijemeDolaska,
                   int cijenaKarte, int minVrijemeCekanja) {
        this.city = city;
        this.imePocetneStanice = imePocetneStanice;
        this.imeKrajnjeStanice = imeKrajnjeStanice;
        this.vrijemePolaska = vrijemePolaska;
        this.vrijemeDolaska = vrijemeDolaska;
        this.cijenaKarte = cijenaKarte;
        this.minVrijemeCekanja = minVrijemeCekanja;
    }
}
