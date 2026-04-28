package com.cotejador.app.desktop;

import com.cotejador.app.data.sqlite.DatabaseInitializer;
import com.cotejador.app.data.sqlite.PdfBoxRuntimeConfig;
import com.cotejador.app.data.sqlite.SQLiteConnectionFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        PdfBoxRuntimeConfig.configure();
        SQLiteConnectionFactory connectionFactory = new SQLiteConnectionFactory();
        new DatabaseInitializer(connectionFactory).initialize();

        FXMLLoader shellLoader = new FXMLLoader(getClass().getResource("/ShellView.fxml"));
        Parent root = shellLoader.load();
        ShellController shellController = shellLoader.getController();
        shellController.initialize();

        Scene scene = new Scene(root, 800, 680);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/theme-dark.css").toExternalForm());

        primaryStage.setTitle("Cotejador");
        primaryStage.setScene(scene);
        shellController.setPrimaryStage(primaryStage);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
