module com.monitor {
    requires javafx.controls;
    requires javafx.graphics;
    requires com.github.oshi;

    exports com.monitor;
    exports com.monitor.view;
    exports com.monitor.viewmodel;
}
