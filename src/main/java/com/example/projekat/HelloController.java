package com.example.projekat;

import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.example.projekat.algorithm.*;
import com.example.projekat.country.*;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.graphstream.graph.Graph;
import org.graphstream.ui.view.Viewer;

/**
 * JavaFX kontroler glavnog prozora aplikacije.
 * <ul>
 *     <li>Popunjava combobox-e i tabele</li>
 *     <li>Racunanje top ruta po kriterijumu</li>
 *     <li>Upis racuna i ispis statistike</li>
 *     <li>Prikaz rute na grafu preko GraphVisualizer</li>
 * </ul>
 * Top-5 alternativa se prikazuje u novom prozoru.
 * @author Gligorije
 */
public class HelloController implements Initializable, Serializable {
    public int brojProdatihKarata;
    public int globalCijena;
    public long vrijemeCekanja;
    public int cijena;

    public Viewer viewer;
    public Country country;
    public Graph g;

    private LocalTime lastQueryTime = null;
    private List<Station> lastMainRoute = Collections.emptyList();

    @FXML public Label labelPocetniGrad;
    @FXML public Label labelKrajnjiGrad;
    @FXML public Label labelKriterijum;
    @FXML public Label labelUkupanBrojKarata;
    @FXML public Label labelUkupnaZarada;

    @FXML public ComboBox<String> comboBoxPocetniGrad;
    @FXML public ComboBox<String> comboBoxKrajnjiGrad;
    @FXML public ComboBox<String> comboBoxKriterijum;

    @FXML public ListView<String> listViewRoute;

    @FXML public Button buttonPretrazi;
    @FXML public Button buttonKupiKartu;
    @FXML public Button buttonPrikazDodatnihRuta;

    /**
     * Kontroler prima model drzave kroz konstruktor.
     *
     * @param country model mreze gradova
     */
    public HelloController(Country country) {
        this.country = country;
    }

    /**
     * Inicijalizacija UI elemenata i izgradnja grafa.
     *
     * @param location URL lokacije resursa (nije koristen)
     * @param resources bundle resursa (nije koristen)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.setProperty("org.graphstream.ui", "swing");

        ObservableList<String> cities_list = FXCollections.observableArrayList(country.getCityNames());
        comboBoxPocetniGrad.setItems(cities_list);
        comboBoxKrajnjiGrad.setItems(cities_list);

        ObservableList<String> path_criteria = FXCollections.observableArrayList("Najbrzi put", "Najmanja cijena", "Najmanje presjedanja");
        comboBoxKriterijum.setItems(path_criteria);

        listViewRoute.visibleProperty().bind(Bindings.isNotEmpty(listViewRoute.getItems()));
        listViewRoute.managedProperty().bind(listViewRoute.visibleProperty());
        listViewRoute.setFixedCellSize(24);
        listViewRoute.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14;");

        listViewRoute.setPrefHeight(listViewRoute.getFixedCellSize() * 10 + 2);
        listViewRoute.setMaxHeight(listViewRoute.getFixedCellSize() * 10 + 2);

        new File("racuni").mkdirs();
        izracunajUkupnuCijenu();
        labelUkupanBrojKarata.setText("Ukupan broj prodatih karata: " + brojProdatihKarata);
        labelUkupnaZarada.setText("Ukupna zarada od prodatih karata: " + globalCijena);

        if (g != null) { g.clear(); g = null; }
        g = GraphVisualizer.buildGraph(country);
    }

    /**
     * Reaguje na promjene combobox-a (pomocne label-e).
     *
     * @param event ActionEvent promjene vrijednosti
     */
    public void writePocetniGrad(ActionEvent event) { labelPocetniGrad.setText(comboBoxPocetniGrad.getValue()); }

    /**
     * Reaguje na promjene combobox-a (pomocne label-e).
     *
     * @param event ActionEvent promjene vrijednosti
     */
    public void writeKrajnjiGrad(ActionEvent event) { labelKrajnjiGrad.setText(comboBoxKrajnjiGrad.getValue()); }

    /**
     * Reaguje na promjene combobox-a (pomocne label-e).
     *
     * @param event ActionEvent promjene vrijednosti
     */
    public void writeKriterijum(ActionEvent event)   { labelKriterijum.setText(comboBoxKriterijum.getValue()); }

    /**
     * Format "h:mm" sa vodecom nulom za minute (npr. 23:04).
     *
     * @param totalMinutes ukupno minuta
     * @return string formata h:mm
     */
    private static String fmtHM(long totalMinutes) {
        long h = totalMinutes / 60;
        long m = totalMinutes % 60;
        return String.format("%d:%02d", h, m);
    }

    /**
     * Popunjava lijevu listu detaljima o ruti i racuna zbirnu cijenu/vrijeme.
     *
     * @param ruta izabrana ruta (lista dionica)
     */
    private void upisiDetaljeULijevuListu(List<Station> ruta) {
        String naslov = String.format("%-19s %-19s %-12s %-12s", "Polazak", "Dolazak", "Tip", "Cijena");
        listViewRoute.getItems().add(naslov);

        int i = 0;
        for (Station s : ruta) {
            String tip = (s.imePocetneStanice.charAt(0) == 'A') ? "Autobus" : "Voz";
            String unos = String.format("%-19s %-19s %-12s %-12s",
                    (s.imePocetneStanice + " (" + s.vrijemePolaska + ")"),
                    (s.imeKrajnjeStanice + " (" + s.vrijemeDolaska + ")"),
                    tip, s.cijenaKarte);

            cijena += s.cijenaKarte;
            listViewRoute.getItems().add(unos);

            if (ruta.size() - 1 > i) {
                long cek = Duration.between(s.vrijemeDolaska, ruta.get(i + 1).vrijemePolaska).toMinutes();
                if (cek < 0) cek += 1440;
                vrijemeCekanja += cek;
            }
            long trajanje = Duration.between(s.vrijemePolaska, s.vrijemeDolaska).toMinutes();
            if (trajanje < 0) trajanje += 1440;
            vrijemeCekanja += trajanje;

            i++;
        }
    }

    /**
     * Komparator za odabrani kriterijum (sa tie-break pravilima).
     *
     * @param kriterijum naziv kriterijuma
     * @return komparator za poredjenje ruta
     */
    private Comparator<List<Station>> cmpFor(String kriterijum){
        if ("Najmanja cijena".equals(kriterijum))  return RouteMetrics.cmpCheapest();
        if ("Najmanje presjedanja".equals(kriterijum)) return RouteMetrics.cmpMinTransfers();
        return RouteMetrics.cmpFastest();
    }

    /**
     * Vraca do 6 ruta po trazenom kriterijumu (Top-K).
     *
     * @param start pocetni grad
     * @param end krajnji grad
     * @param kriterijum naziv kriterijuma
     * @return lista ruta
     */
    private List<List<Station>> topK(City start, City end, String kriterijum) {
        int K = 6;
        if ("Najbrzi put".equals(kriterijum)) {
            LocalTime now = (lastQueryTime != null) ? lastQueryTime : LocalTime.now();
            return KBestRoutes.topKFastest(country, start, end, now, K);
        } else if ("Najmanja cijena".equals(kriterijum)) {
            return KBestRoutes.topKCheapest(country, start, end, K);
        } else if ("Najmanje presjedanja".equals(kriterijum)) {
            return KBestRoutes.topKMinTransfers(country, start, end, K);
        }
        return Collections.emptyList();
    }

    /**
     * Klik na "Pretrazi": pronalazi najbolju rutu po kriterijumu
     * i prikazuje je u tabeli i na grafu.
     *
     * @param event klik dogadjaj
     */
    public void findShortestPath(ActionEvent event) {
        cijena = 0;
        vrijemeCekanja = 0;
        listViewRoute.getItems().clear();

        String gradOd = comboBoxPocetniGrad.getValue();
        String gradDo = comboBoxKrajnjiGrad.getValue();
        String kriterijum = comboBoxKriterijum.getValue();

        City start = country.getCityByCityName(gradOd);
        City end   = country.getCityByCityName(gradDo);
        if (start == null || end == null) {
            new Alert(Alert.AlertType.WARNING, "Molim odaberite ispravne gradove.").showAndWait();
            return;
        }
        if (start.name.equals(end.name)) {
            listViewRoute.getItems().clear();
            new Alert(Alert.AlertType.INFORMATION,
                    "Pocetni i krajnji grad su isti. Ruta nije potrebna.").showAndWait();
            return;
        }
        lastQueryTime = LocalTime.now();

        List<List<Station>> routes = topK(start, end, kriterijum);
        if (routes.isEmpty()) {
            List<Station> one;
            if ("Najbrzi put".equals(kriterijum)) {
                one = new Dijkstra().nadjiNajkracuRutu(country, start, end, lastQueryTime);
            } else if ("Najmanja cijena".equals(kriterijum)) {
                one = new DijkstraCijena().nadjiNajjeftinijuRutu(country, start, end);
            } else {
                one = new BFSMinPresjedanja().nadjiRutuMinPresjedanja(country, start, end);
            }
            if (one != null && !one.isEmpty()) {
                routes = new ArrayList<>();
                routes.add(one);
            }
        }

        if (routes.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Nije pronadjena ruta.").showAndWait();
            return;
        }

        routes.sort(cmpFor(kriterijum));
        List<Station> ruta = routes.get(0);
        lastMainRoute = ruta;

        upisiDetaljeULijevuListu(ruta);
        long totalMin = RouteMetrics.totalRouteDuration(ruta);
        int totalPrice = RouteMetrics.totalPrice(ruta);
        int hops = RouteMetrics.transfers(ruta);
        listViewRoute.getItems().add(String.format(
                "Ukupno: vrijeme=%sh | cijena=%d | presjedanja=%d",
                fmtHM(totalMin), totalPrice, hops));

        GraphVisualizer.renderRouteAsync(country, ruta, end);
    }

    /**
     * Klik na "Prikazi dodatne rute": prikazuje do 5 alternativa.
     *
     * @param e klik dogadjaj
     */
    public void prikaziAlternativneRute(ActionEvent e) {
        String gradOd = comboBoxPocetniGrad.getValue();
        String gradDo = comboBoxKrajnjiGrad.getValue();
        String kriterijum = comboBoxKriterijum.getValue();

        City start = country.getCityByCityName(gradOd);
        City end   = country.getCityByCityName(gradDo);
        if (start == null || end == null) return;
        if (start.name.equals(end.name)) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Pocetni i krajnji grad su isti. Nema alternativnih ruta.").showAndWait();
            return;
        }
        List<List<Station>> routes = topK(start, end, kriterijum);
        if (routes.isEmpty()) return;

        routes.sort(cmpFor(kriterijum));

        List<List<Station>> alts = new ArrayList<>();
        for (List<Station> r : routes) if (!sameRoute(r, lastMainRoute)) alts.add(r);
        if (alts.size() > 5) alts = alts.subList(0, 5);

        showAlternativesWindow(alts, kriterijum);
    }

    /**
     * Poredjenje dvije rute po identicnosti (isti niz dionica i vremena).
     *
     * @param a prva ruta
     * @param b druga ruta
     * @return true ako su identicne
     */
    private boolean sameRoute(List<Station> a, List<Station> b) {
        if (a == null || b == null || a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Station x = a.get(i), y = b.get(i);
            if (!x.imePocetneStanice.equals(y.imePocetneStanice)) return false;
            if (!x.imeKrajnjeStanice.equals(y.imeKrajnjeStanice)) return false;
            if (!x.vrijemePolaska.equals(y.vrijemePolaska)) return false;
            if (!x.vrijemeDolaska.equals(y.vrijemeDolaska)) return false;
        }
        return true;
    }

    /**
     * Otvara novi prozor sa Top-5 rutama.
     *
     * @param alts liste alternativnih ruta
     * @param kriterijum naziv kriterijuma
     */
    private void showAlternativesWindow(List<List<Station>> alts, String kriterijum) {
        ListView<String> lv = new ListView<>();
        lv.setFixedCellSize(24);
        lv.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14;");
        lv.getItems().add(String.format("Top 5 ruta (%s)", kriterijum));

        int idx = 1;
        for (List<Station> r : alts) {
            int price = RouteMetrics.totalPrice(r);
            int hops  = RouteMetrics.transfers(r);
            long minutes = RouteMetrics.totalRouteDuration(r);
            lv.getItems().add(String.format("Ruta #%d | vrijeme: %sh | cijena: %d | presjedanja: %d",
                    idx++, fmtHM(minutes), price, hops));

        }

        VBox root = new VBox(8, lv);
        Stage stage = new Stage();
        stage.setTitle("Dodatne rute");
        stage.setScene(new Scene(root, 600, 300));
        stage.show();
    }

    /**
     * Kreira TXT racun za prikazanu rutu i azurira sumarne brojke.
     *
     * @param event klik dogadjaj
     */
    public void kupovinaKarte(ActionEvent event) {
        if (vrijemeCekanja <= 0 && cijena <= 0) {
            new Alert(Alert.AlertType.INFORMATION, "Nema rute za kupiti.").showAndWait();
            return;
        }

        String gradOd = comboBoxPocetniGrad.getValue();
        String gradDo = comboBoxKrajnjiGrad.getValue();

        File dir = new File("racuni");
        if (!dir.exists()) dir.mkdirs();

        DateTimeFormatter fmtName = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        DateTimeFormatter fmtTs   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String stamp = LocalDateTime.now().format(fmtName);
        File file = new File(dir, stamp + ".txt");

        String ts = LocalDateTime.now().format(fmtTs);
        String sadrzaj = ""
                + "Relacija: " + gradOd + " -> " + gradDo + System.lineSeparator()
                + "UkupnoVrijeme: " + fmtHM(vrijemeCekanja) + System.lineSeparator()
                + "Cijena: " + cijena + System.lineSeparator()
                + "Vrijeme: " + ts + System.lineSeparator();

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, false)))) {
            pw.print(sadrzaj);
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Greska pri snimanju: " + ex.getMessage()).showAndWait();
            return;
        }

        izracunajUkupnuCijenu();
        labelUkupanBrojKarata.setText("Ukupan broj prodatih karata: " + brojProdatihKarata);
        labelUkupnaZarada.setText("Ukupna zarada od prodatih karata: " + globalCijena);

        new Alert(Alert.AlertType.INFORMATION, "Racun sacuvan u fajl: " + file.getName()).showAndWait();
    }

    /**
     * Ucitava sve TXT racune iz foldera "racuni"
     * i racuna zbirnu cijenu i broj prodatih karata.
     */
    public void izracunajUkupnuCijenu() {
        globalCijena = 0;
        brojProdatihKarata = 0;

        File dir = new File("racuni");
        if (!dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) return;

        for (File f : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                Integer price = null;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("Cijena:")) {
                        String num = line.substring("Cijena:".length()).trim();
                        price = Integer.parseInt(num);
                        break;
                    }
                }
                if (price != null) {
                    globalCijena += price;
                    brojProdatihKarata++;
                }
            } catch (Exception ex) {
                System.err.println("Preskacem TXT " + f.getName() + ": " + ex.getMessage());
            }
        }
    }
}
