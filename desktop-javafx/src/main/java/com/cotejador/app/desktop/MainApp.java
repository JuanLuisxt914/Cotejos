package com.cotejador.app.desktop;

import com.cotejador.app.data.sqlite.DatabaseInitializer;
import com.cotejador.app.data.sqlite.EventoRepository;
import com.cotejador.app.data.sqlite.GalloRepository;
import com.cotejador.app.data.sqlite.PartidoRepository;
import com.cotejador.app.data.sqlite.RestriccionPartidoRepository;
import com.cotejador.app.data.sqlite.SQLiteConnectionFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        SQLiteConnectionFactory connectionFactory = new SQLiteConnectionFactory();
        new DatabaseInitializer(connectionFactory).initialize();
        EventoRepository eventoRepository = new EventoRepository(connectionFactory);
        PartidoRepository partidoRepository = new PartidoRepository(connectionFactory);
        GalloRepository galloRepository = new GalloRepository(connectionFactory);
        RestriccionPartidoRepository restriccionPartidoRepository =
                new RestriccionPartidoRepository(connectionFactory);
        MainController controller = new MainController(
                eventoRepository,
                partidoRepository,
                galloRepository,
                restriccionPartidoRepository);

        Scene scene = new Scene(controller.createView(), 1380, 600);

        primaryStage.setTitle("Cotejador");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
