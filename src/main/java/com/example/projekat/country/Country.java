package com.example.projekat.country;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Model drzave kao matrice gradova sa pripadajucim stanicama i polascima.
 * <p>
 * Zaduzena je za ucitavanje gradova i njihovih polazaka iz JSON fajla koji je
 * generisao {@code TransportDataGenerator}.
 * </p>
 * @author Gligorije
 */
public class Country {
    /** Broj redova matrice gradova. */
    public int SIZE_N;
    /** Broj kolona matrice gradova. */
    public int SIZE_M;
    /** Lista svih gradova u drzavi. */
    public ArrayList<City> cities = new ArrayList<>();

    /**
     * @param SIZE_N broj redova
     * @param SIZE_M broj kolona
     */
    public Country(int SIZE_N, int SIZE_M) {
        this.SIZE_N = SIZE_N;
        this.SIZE_M = SIZE_M;
    }
    public static File jsonFile() {
        String override = System.getProperty("transport.json.path");
        if (override != null && !override.isBlank()) return new File(override);
        File dir = new File(System.getProperty("user.home"), ".projekat");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "transport_data.json");
    }

    /**
     * Vraca grad po imenu (npr. {@code G_1_2}) samo ako ima ucitane stanice.
     *
     * @param cityName ime grada
     * @return grad ili {@code null} ako nije pronadjen ili nema stanica
     */
    public City getCityByCityName(String cityName) {
        for (City c : cities) {
            if (c.name.equals(cityName) && !c.stations.isEmpty()) {
                return c;
            }
        }
        return null;
    }

    /**
     * Ucitava gradove i njihove stanice/polaske iz
     * {@code src/main/java/com/example/projekat/generator/transport_data.json}.
     *
     * @throws Exception ako JSON nije validan ili dodje do IO greske
     */
    public void ucitavanjeDrzave() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(Country.jsonFile());
        if (root == null) {
            throw new IllegalStateException("Nije moguce ucitati JSON fajl.");
        }
        for (JsonNode oneNode : root.get("stations")) {
            String CityName = oneNode.get("city").asText();
            String busName = oneNode.get("busStation").asText();
            String trainName = oneNode.get("trainStation").asText();
            City newCity = new City(this, CityName, busName, trainName);
            cities.add(newCity);
        }
        for (City c : cities) {
            try {
                c.ucitavanjeStanicaZaGrad();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Pronalazi grad po imenu.
     *
     * @param grad ime grada (npr. {@code G_2_3})
     * @return instanca grada ili {@code null} ako nije pronadjen
     */
    public City nadjiGradPoImenu(String grad) {
        for (City c : cities) {
            if (c.name.equals(grad)) {
                return c;
            }
        }
        return null;
    }

    /**
     * @return lista svih imena gradova (redoslijed kao u {@link #cities})
     */
    public List<String> getCityNames() {
        List<String> cityNamse = new ArrayList<>();
        for (City city : cities) {
            cityNamse.add(city.name);
        }
        return cityNamse;
    }

    /**
     * @return spisak svih polazaka (stanica/dionica) u drzavi
     */
    public List<Station> getAllStations() {
        List<Station> stations = new ArrayList<>();
        for (City c : cities) {
            stations.addAll(c.stations);
        }
        return stations;
    }
}
