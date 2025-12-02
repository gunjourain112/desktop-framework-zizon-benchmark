package ui

import (
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"

	"apps/04-fyne/monitor"
)

// CreateDashboard constructs the main UI and returns the widgets needed for updates.
// Returns:
// - container: The main layout object
// - chartWidget: The CPU history chart
// - donutWidget: The Memory usage donut
// - memLabel: The text label for memory percentage
func CreateDashboard(m *monitor.MonitorService) (
	fyne.CanvasObject,
	*ChartWidget,
	*DonutWidget,
	*canvas.Text,
) {
	// CPU Chart
	chartWidget := NewChartWidget(m)
	chartContainer := container.NewPadded(chartWidget)

	chartBox := container.NewBorder(
		widget.NewLabelWithStyle("CPU History", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		nil, nil, nil,
		chartContainer,
	)

	// Memory Donut
	donutWidget := NewDonutWidget(m)
	memLabel := canvas.NewText("0%", nil)
	memLabel.Alignment = fyne.TextAlignCenter
	memLabel.TextSize = 24

	// Overlay label on top of donut
	donutContent := container.NewStack(donutWidget, container.NewCenter(memLabel))

	donutBox := container.NewBorder(
		widget.NewLabelWithStyle("Memory Usage", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		nil, nil, nil,
		donutContent,
	)

	// Grid layout
	grid := container.NewGridWithColumns(2, chartBox, donutBox)

	return grid, chartWidget, donutWidget, memLabel
}
