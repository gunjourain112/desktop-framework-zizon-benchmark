package com.monitor.view;

import com.monitor.viewmodel.SystemViewModel;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class DashboardView extends Pane {
    private final SystemViewModel viewModel;
    private final Canvas canvas;

    // Theme Colors
    private static final Color BG_COLOR = Color.web("#1E1E1E");
    private static final Color CPU_COLOR = Color.CYAN;
    private static final Color MEM_COLOR = Color.MAGENTA;
    private static final Color TEXT_COLOR = Color.WHITE;

    // Layout Constants
    private static final double MARGIN = 40;
    private static final double TITLE_FONT_SIZE = 20;
    private static final double TITLE_OFFSET = 10;
    private static final double TEXT_POSITION_OFFSET = 60;
    private static final double DONUT_OUTER_RATIO = 0.8;
    private static final double DONUT_INNER_RATIO = 0.6;

    public DashboardView(SystemViewModel viewModel) {
        this.viewModel = viewModel;
        this.canvas = new Canvas();
        getChildren().add(canvas);

        // Redraw when data updates
        // viewModel.currentCpuLoadProperty().addListener(obs -> draw());
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        draw();
    }

    private void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();

        // 1. Background
        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, w, h);

        // 2. Layout Calculation
        // Split screen: Left for CPU (Line Chart), Right for Memory (Donut)
        double chartW = (w - MARGIN * 3) / 2;
        double chartH = h - MARGIN * 2;
        double leftX = MARGIN;
        double rightX = MARGIN * 2 + chartW;
        double topY = MARGIN;

        drawCpuChart(gc, leftX, topY, chartW, chartH);
        drawMemoryChart(gc, rightX, topY, chartW, chartH);
    }

    private void drawCpuChart(GraphicsContext gc, double x, double y, double w, double h) {
        // Title
        gc.setFill(TEXT_COLOR);
        gc.setFont(new Font("Arial", TITLE_FONT_SIZE));
        gc.fillText("CPU Load (60s)", x, y - TITLE_OFFSET);

        // Border
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, w, h);

        double[] history = viewModel.getCpuHistory();
        int startIndex = viewModel.getCpuHistoryIndex(); // Ring buffer: this is the oldest value
        int count = history.length;
        double stepX = w / (count - 1);

        // Draw Line - reading from ring buffer (oldest to newest)
        gc.setStroke(CPU_COLOR);
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i < count; i++) {
            // Read from ring buffer: start at startIndex (oldest) and wrap around
            int bufferIndex = (startIndex + i) % count;
            double val = history[bufferIndex]; // 0.0 to 1.0
            double px = x + i * stepX;
            double py = y + h - (val * h); // Invert Y

            if (i == 0) gc.moveTo(px, py);
            else gc.lineTo(px, py);
        }
        gc.stroke();

        // Current Value Text
        String currentVal = String.format("%.1f%%", viewModel.currentCpuLoadProperty().get() * 100);
        gc.fillText(currentVal, x + w - TEXT_POSITION_OFFSET, y + 20);
    }

    private void drawMemoryChart(GraphicsContext gc, double x, double y, double w, double h) {
        // Title
        gc.setFill(TEXT_COLOR);
        gc.setFont(new Font("Arial", TITLE_FONT_SIZE));
        gc.fillText("Memory Usage", x, y - TITLE_OFFSET);

        // Center of donut
        double centerX = x + w / 2;
        double centerY = y + h / 2;
        double radius = Math.min(w, h) / 2 * DONUT_OUTER_RATIO;

        // Data
        double usedPct = viewModel.memoryUsageProperty().get();
        double usedAngle = usedPct * 360;

        // Draw Background Circle (Empty)
        gc.setFill(Color.DARKGRAY);
        gc.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 0, 360, javafx.scene.shape.ArcType.ROUND);

        // Draw Used Arc
        gc.setFill(MEM_COLOR);
        gc.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, -usedAngle, javafx.scene.shape.ArcType.ROUND);

        // Cut out center to make it a Donut
        double innerRadius = radius * DONUT_INNER_RATIO;
        gc.setFill(BG_COLOR);
        gc.fillOval(centerX - innerRadius, centerY - innerRadius, innerRadius * 2, innerRadius * 2);

        // Text in Center
        gc.setFill(TEXT_COLOR);
        String text = String.format("%.1f%%", usedPct * 100);

        // Simple centering approximation
        double textWidth = text.length() * 10;
        gc.fillText(text, centerX - textWidth/2, centerY + 8);
    }
}
