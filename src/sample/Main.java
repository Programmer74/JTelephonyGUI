package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));


        primaryStage.setTitle("JT");
        primaryStage.setScene(new Scene(root, 300, 300));
        primaryStage.setOnCloseRequest(e -> {Platform.exit(); System.exit(0);});
        primaryStage.setOnShown(e -> MainController.formShown = true);
        primaryStage.setResizable(false);

        primaryStage.show();


        //Controller c = new Controller();
        //c.updateServerIP();
        //Controller.updateServerIP();
}


    public static void main(String[] args) {
        launch(args);
    }
}
