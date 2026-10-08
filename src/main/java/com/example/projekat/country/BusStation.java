package com.example.projekat.country;

import java.time.LocalTime;

/**
 * Specijalizacija {@link Station} za autobuske polaske.
 * @author Gligorije
 */
public class BusStation extends Station {

    /**
     * Kreira autobusku dionicu.
     *
     * @param city               grad kojem pripada polazak
     * @param imePocetneStanice  oznaka pocetne stanice (A_x_y)
     * @param imeKrajnjeStanice  oznaka krajnje stanice (A_x_y)
     * @param vrijemePolaska     vrijeme polaska
     * @param vrijemeDolaska     vrijeme dolaska
     * @param cijenaKarte        cijena karte
     * @param minVrijemeCekanja  minimalno vrijeme cekanja prije naredne dionice
     */
    public BusStation(City city, String imePocetneStanice, String imeKrajnjeStanice,
                      LocalTime vrijemePolaska, LocalTime vrijemeDolaska,
                      int cijenaKarte, int minVrijemeCekanja) {
        super(city, imePocetneStanice, imeKrajnjeStanice, vrijemePolaska, vrijemeDolaska, cijenaKarte, minVrijemeCekanja);
    }
}
