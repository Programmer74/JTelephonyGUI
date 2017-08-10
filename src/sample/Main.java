package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));


        primaryStage.setTitle("JT");
        primaryStage.setScene(new Scene(root, 300, 300));
        primaryStage.setOnCloseRequest(e -> {Platform.exit(); System.exit(0);});
        primaryStage.setOnShown(e -> Controller.formShown = true);
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
