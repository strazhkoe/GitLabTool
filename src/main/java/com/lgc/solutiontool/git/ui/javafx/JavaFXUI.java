package com.lgc.solutiontool.git.ui.javafx;

import com.lgc.solutiontool.git.services.LoginService;
import com.lgc.solutiontool.git.services.ServiceProvider;
import com.lgc.solutiontool.git.ui.UserInterface;
import com.lgc.solutiontool.git.ui.ViewKey;
import com.lgc.solutiontool.git.ui.javafx.controllers.ModularController;
import com.lgc.solutiontool.git.ui.javafx.dto.DialogDTO;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

public class JavaFXUI extends Application implements UserInterface {
    private Image appIcon;

    @Override
    public void run(String[] args) {
        launch(args);
    }

    private LoginService _loginService =
            (LoginService) ServiceProvider.getInstance().getService(LoginService.class.getName());

    @Override
    public void start(Stage primaryStage) throws Exception {
        appIcon = new Image(getClass().getClassLoader().getResource("icons/gitlab.png").toExternalForm());

        URL modularWindow = getClass().getClassLoader().getResource(ViewKey.MODULAR_CONTAINER.getPath());
        if (modularWindow == null) {
            System.out.println("ERROR: Could not load fxml resource");
            return;
        }

        showloginDialog();
        FXMLLoader fxmlLoader = new FXMLLoader(modularWindow);
        Parent root = fxmlLoader.load();

        ModularController modularController = fxmlLoader.getController();
        modularController.loadWelcomeWindow();

        Scene scene = new Scene(root);

        primaryStage.setTitle("Solution Tool for GitLab");
        primaryStage.setScene(scene);
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setHeight(primaryScreenBounds.getMaxY() / 1.5);
        primaryStage.setWidth(primaryScreenBounds.getMaxX() / 1.5);
        primaryStage.getIcons().add(appIcon);
        primaryStage.show();

    }

    private void showloginDialog() {
        LoginDialog ld = new LoginDialog();
        Stage stage = (Stage) ld.getDialogPane().getScene().getWindow();
        stage.getIcons().add(appIcon);

        DialogDTO dialogParams = ld.showAndWait().orElseThrow(() -> new RuntimeException("Error in LoginDialog"));
        _loginService.login(dialogParams.getServerURL(), dialogParams.getLogin(), dialogParams.getPassword());
    }
}
