package main

import (
	"fmt"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"

	"apps/04-fyne/monitor"
	"apps/04-fyne/ui"
)

func main() {
	a := app.New()
	w := a.NewWindow("System Monitor (Go 1.24 + Fyne)")

	// Init Monitor (60s history)
	mon := monitor.NewMonitorService(60)

	// Create UI using the shared layout builder
	content, chartWidget, donutWidget, memLabel := ui.CreateDashboard(mon)

	w.SetContent(content)
	w.Resize(fyne.NewSize(800, 400))

	// Start Monitor
	mon.Start(1*time.Second, func() {
		// This callback runs in the monitor's background goroutine.
		// We must update UI elements on the main thread.

		// Capture current memory usage to display (simple read)
		memUsage := mon.Stats.MemoryUsage

		// Schedule UI update
		if drv := a.Driver(); drv != nil {
			// DoFromGoroutine signature requires a boolean (likely for blocking/sync behavior in some driver implementations)
			// Passing false as we don't need to block the monitor loop.
			// Note: The signature in v2.5.4 seems to include this second arg, which might be internal or specific.
			// However, if the interface demands it, we provide it.
			drv.DoFromGoroutine(func() {
				chartWidget.Refresh()
				donutWidget.Refresh()
				memLabel.Text = fmt.Sprintf("%.1f%%", memUsage)
				memLabel.Refresh()
			})
		}
	})

	w.SetOnClosed(func() {
		mon.Stop()
	})
	w.ShowAndRun()
}
