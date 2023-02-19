package com.example.rbtree;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Image icon = new Image("iconRBTree.png");
        stage.getIcons().add(icon);
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/rbtree/RBtree.fxml")));
        Scene startScene = new Scene(root);

        stage.setScene(startScene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}