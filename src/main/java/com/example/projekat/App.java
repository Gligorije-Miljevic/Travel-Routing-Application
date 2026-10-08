package com.example.projekat;

import com.example.projekat.algorithm.GraphVisualizer;
import com.example.projekat.country.Country;
import com.example.projekat.generator.TransportDataGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.control.Separator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

/**
 * JavaFX ulazna tacka aplikacije.
 * <ul>
 *     <li>Generise ulazni JSON (transport_data.json)</li>
 *     <li>Ucitava mapu drzave i polaske</li>
 *     <li>Pokrece glavni UI (HelloController)</li>
 * </ul>
 * @author Gligorije
 */
public class App extends Application {
    public static int SIZE_N;
    public static int SIZE_M;
    public static int Num_Of_Cities;
    public static int Num_Of_Departures;
/*
    private static int[] ucitajStatistikuRacunaTxt() {
        int count = 0;
        int suma  = 0;
        File dir = new File("racuni");
        if (!dir.isDirectory()) return new int[]{0, 0};

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) return new int[]{0, 0};

        for (File f : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line; Integer price = null;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("Cijena:")) {
                        String num = line.substring("Cijena:".length()).trim();
                        price = Integer.parseInt(num);
                        break;
                    }
                }
                if (price != null) { suma += price; count++; }
            } catch (Exception ex) {
                System.err.println("Preskacem TXT " + f.getName() + ": " + ex.getMessage());
            }
        }
        return new int[]{count, suma};
    }*/

    /**
     * Ucitava osnovne parametre iz JSON fajla i postavlja globalne dimenzije.
     *
     * @throws IOException ako JSON ne moze biti ucitan
     */
    public static void opsteUcitavanjeJSona() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(com.example.projekat.country.Country.jsonFile());
        if (root == null) throw new IllegalStateException("Nije moguce ucitati JSON fajl.");

        JsonNode countryMap = root.get("countryMap");
        if (countryMap == null || !countryMap.isArray())
            throw new IllegalStateException("Mapa drzave nije validna.");

        SIZE_N = countryMap.size();
        SIZE_M = countryMap.get(0).size();
        Num_Of_Cities = SIZE_N * SIZE_M;

        JsonNode departures = root.get("departures");
        if (departures == null || !departures.isArray())
            throw new IllegalStateException("Prevozna sredstva nisu dostupna.");

        Num_Of_Departures = departures.size() / Num_Of_Cities / 2;
    }

    /**
     * Kreiranje i prikaz glavnog prozora.
     *
     * @param primaryStage primarni JavaFX stage aplikacije
     * @throws Exception u slucaju problema sa ucitavanjem FXML ili generisanjem podataka
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        //Optional<int[]> dimsOpt = askDims(primaryStage);
        //if (dimsOpt.isEmpty()) return;
       // int[] dims = dimsOpt.get();
       // int n = dims[0], m = dims[1];

        File json = com.example.projekat.country.Country.jsonFile();
        json.getParentFile().mkdirs();
        //new TransportDataGenerator(10, 9).generateToFile(json.getAbsolutePath());
        opsteUcitavanjeJSona();

        Country country = new Country(SIZE_N, SIZE_M);
        country.ucitavanjeDrzave();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
        loader.setController(new HelloController(country));
        Parent root = loader.load();

        primaryStage.setTitle("");
        primaryStage.setScene(new Scene(root));
        Platform.setImplicitExit(true);
        primaryStage.setOnCloseRequest(e -> {
            try { GraphVisualizer.shutdown(); } catch (Exception ignored) {}
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    /**
     * Dijalog za unos dimenzija matrice (n x m).
     *
     * @param owner prozor nad kojim se prikazuje ovaj dijalog (moze biti null)
     * @return izabrane dimenzije [n,m] ili prazno ako je otkazano
     */
    /*private Optional<int[]> askDims(Stage owner) {
        Dialog<int[]> dlg = new Dialog<>();
        if (owner != null && owner.getScene() != null) {
            dlg.initOwner(owner);
        }

        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Dimenzije matrice (n x m)");

        ButtonType nastavi = new ButtonType("Nastavi", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(nastavi, ButtonType.CANCEL);

        //int[] stats = ucitajStatistikuRacunaTxt();
        Label lblCount = new Label("Ukupan broj prodatih karata: " + stats[0]);
        Label lblSum   = new Label("Ukupna zarada od prodatih karata: " + stats[1]);

        TextField tfN = new TextField();
        TextField tfM = new TextField();
        tfN.setPromptText("n");
        tfM.setPromptText("m");
        tfN.textProperty().addListener((o,a,b)->{ if(!b.matches("\\d*")) tfN.setText(b.replaceAll("\\D",""));});
        tfM.textProperty().addListener((o,a,b)->{ if(!b.matches("\\d*")) tfM.setText(b.replaceAll("\\D",""));});

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.addRow(0, new Label("n (redovi):"), tfN);
        gp.addRow(1, new Label("m (kolone):"), tfM);

        VBox content = new VBox(10, lblCount, lblSum, new Separator(), gp);
        dlg.getDialogPane().setContent(content);

        dlg.setResultConverter(btn -> {
            if (btn == nastavi) {
                try {
                    int n = Integer.parseInt(tfN.getText());
                    int m = Integer.parseInt(tfM.getText());
                    if (n <= 0 || m <= 0) return null;
                    return new int[]{n, m};
                } catch (Exception ignored) { return null; }
            }
            return null;
        });

        return dlg.showAndWait();
    }*/

    /**
     * Ulazna tacka aplikacije.
     *
     * @param args argumenti komandne linije
     */
    public static void main(String[] args) {
        launch(args);
    }
}
