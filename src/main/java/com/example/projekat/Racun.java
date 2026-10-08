package com.example.projekat;

import java.io.Serializable;

/**
 * Jednostavan model racuna (potvrde o kupovini rute).
 * <p>
 * Instanca se serijalizuje u fajl unutar foldera {@code racuni/}
 * i sadrzi relaciju, ukupno trajanje putovanja, cijenu i trenutno vrijeme.
 * </p>
 *
 * @author Gligorije
 */
public class Racun implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Pocetni grad (npr. {@code "G_0_0"}). */
    public String gradOd;

    /** Odredisni grad (npr. {@code "G_3_4"}). */
    public String gradDo;

    /** Ukupno trajanje putovanja u minutama (voznja + cekanja). */
    public long ukupnoVrijemePutovanja;

    /** Ukupna cijena putovanja. */
    public int cijenaPutovanja;

    /** Datum i vrijeme kreiranja racuna u formatu npr. {@code "yyyy-MM-dd HH:mm:ss"}. */
    public String trenutnoVrijeme;

    /**
     * Kreira novi racun.
     *
     * @param gradOd pocetni grad
     * @param gradDo odredisni grad
     * @param ukupnoVrijemePutovanja ukupno trajanje putovanja (minute)
     * @param cijenaPutovanja ukupna cijena
     * @param trenutnoVrijeme timestamp kreiranja (formatiran string)
     */
    public Racun(String gradOd,
                 String gradDo,
                 long ukupnoVrijemePutovanja,
                 int cijenaPutovanja,
                 String trenutnoVrijeme) {
        this.gradOd = gradOd;
        this.gradDo = gradDo;
        this.ukupnoVrijemePutovanja = ukupnoVrijemePutovanja;
        this.cijenaPutovanja = cijenaPutovanja;
        this.trenutnoVrijeme = trenutnoVrijeme;
    }

    /**
     * Kratak, citljiv prikaz sadrzaja racuna (korisno za logove).
     *
     * @return string sa osnovnim poljima racuna
     */
    @Override
    public String toString() {
        return gradOd + " " + gradDo + " " + ukupnoVrijemePutovanja + " " + cijenaPutovanja + " " + trenutnoVrijeme;
    }
}
