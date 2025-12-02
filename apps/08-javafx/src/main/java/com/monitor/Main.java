package com.monitor;

import com.monitor.view.DashboardView;
import com.monitor.viewmodel.SystemViewModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private SystemViewModel viewModel;

    @Override
    public void start(Stage primaryStage) {
        viewModel = new SystemViewModel();
        viewModel.start();

        DashboardView root = new DashboardView(viewModel);
        Scene scene = new Scene(root, 1280, 800);

        primaryStage.setTitle("JavaFX System Monitor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (viewModel != null) {
            viewModel.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
