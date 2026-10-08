package com.example.projekat.country;

import java.time.LocalTime;

/**
 * Specijalizacija {@link Station} za zeljeznicke polaske.
 * @author Gligorije
 */
public class TrainStation extends Station {

    /**
     * Kreira zeljeznicku dionicu.
     *
     * @param city               grad kojem pripada polazak
     * @param imePocetneStanice  oznaka popetne stanice (Z_x_y)
     * @param imeKrajnjeStanice  oznaka krajnje stanice (Z_x_y)
     * @param vrijemePolaska     vrijeme polaska
     * @param vrijemeDolaska     vrijeme dolaska
     * @param cijenaKarte        cijena karte
     * @param minVrijemeCekanja  minimalno vrijeme cekanja prije naredne dionice
     */
    public TrainStation(City city, String imePocetneStanice, String imeKrajnjeStanice,
                        LocalTime vrijemePolaska, LocalTime vrijemeDolaska,
                        int cijenaKarte, int minVrijemeCekanja) {
        super(city, imePocetneStanice, imeKrajnjeStanice, vrijemePolaska, vrijemeDolaska, cijenaKarte, minVrijemeCekanja);
    }
}
