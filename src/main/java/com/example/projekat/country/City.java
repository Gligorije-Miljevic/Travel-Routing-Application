package com.example.projekat.country;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Grad sa pripadajucim stanicama i polascima.
 * <p>
 * Drzi nazive glavnih stanica (autobuska/zeljeznicka) i listu konkretnih dionica
 * (polazaka) koje krecu iz ovog grada. Pri ucitavanju povezuje i "susjede"
 * tj. polaske koji su dostupni nakon dolaska u krajnji grad.
 * </p>
 * @author Gligorije
 */
public class City {
    /** Sve dionice (polasci) koje polaze iz ovog grada. */
    public ArrayList<Station> stations = new ArrayList<>();
    /** Drzava kojoj grad pripada. */
    public Country country;
    /** Ime grada (npr. {@code G_1_2}). */
    public String name;
    /** Oznaka glavne autobuske stanice (npr. {@code A_1_2}). */
    public String busStationName;
    /** Oznaka glavne zeljeznicke stanice (npr. {@code Z_1_2}). */
    public String trainStationName;

    /**
     * @param country          referenca na drzavu
     * @param name             ime grada (G_x_y)
     * @param busStationName   oznaka autobuske stanice (A_x_y)
     * @param trainStationName oznaka zeljeznicke stanice (Z_x_y)
     */
    public City(Country country, String name, String busStationName, String trainStationName) {
        this.country = country;
        this.name = name;
        this.busStationName = busStationName;
        this.trainStationName = trainStationName;
    }

    /**
     * Ucitava dionice (polaske) za ovaj grad iz JSON-a i povezuje susjede.
     * <p>
     * Za svaki zapis iz sekcije {@code departures} gdje {@code from} odgovara
     * autobuskoj ili zeljeznickoj stanici ovog grada:
     * <ul>
     *     <li>parsa se vrijeme polaska, racuna se vrijeme dolaska (polazak + duration)</li>
     *     <li>kreira se {@link BusStation} ili {@link TrainStation}</li>
     *     <li>dodaje se u listu {@link #stations}</li>
     *     <li>pronalazi se ciljni grad i povezuju se susjedne dionice (sljedeci polasci)</li>
     * </ul>
     *
     * @throws Exception ako JSON nije dostupan ili je neispravan
     */
    public void ucitavanjeStanicaZaGrad() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(Country.jsonFile());
        if (root == null) {
            throw new IllegalStateException("Nije moguce ucitati JSON fajl.");
        }

        for (JsonNode oneNode : root.get("departures")) {
            if (oneNode.get("from").asText().equals(busStationName)
                    || oneNode.get("from").asText().equals(trainStationName)) {

                String imePocetneStanice = oneNode.get("from").asText();
                String imeKrajnjegGrada = oneNode.get("to").asText();
                String imeKrajnjeStanice;
                if (imePocetneStanice.charAt(0) == 'A') {
                    imeKrajnjeStanice = "A" + imeKrajnjegGrada.substring(1);
                } else if (imePocetneStanice.charAt(0) == 'Z') {
                    imeKrajnjeStanice = "Z" + imeKrajnjegGrada.substring(1);
                } else {
                    imeKrajnjeStanice = "U" + imeKrajnjegGrada.substring(1);
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                LocalTime vrijemePolaska = LocalTime.parse(oneNode.get("departureTime").asText(), formatter);
                LocalTime vrijemeDolaska = vrijemePolaska.plusMinutes(oneNode.get("duration").asInt());
                int cijenaKarte = oneNode.get("price").asInt();
                int minVrijemeCekanja = oneNode.get("minTransferTime").asInt();

                Station pomocnaStanica;
                if (oneNode.get("type").asText().equals("autobus")) {
                    pomocnaStanica = new BusStation(this, imePocetneStanice, imeKrajnjeStanice,
                            vrijemePolaska, vrijemeDolaska, cijenaKarte, minVrijemeCekanja);
                } else {
                    pomocnaStanica = new TrainStation(this, imePocetneStanice, imeKrajnjeStanice,
                            vrijemePolaska, vrijemeDolaska, cijenaKarte, minVrijemeCekanja);
                }
                stations.add(pomocnaStanica);
                City targetCity = this.country.nadjiGradPoImenu(imeKrajnjegGrada);
                if (targetCity != null) {
                    for (Station s : targetCity.stations) {
                        if (s.imePocetneStanice.equals(pomocnaStanica.imeKrajnjeStanice)) {
                            pomocnaStanica.addNeighbour(s);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Ime grada je " + name;
    }
}
