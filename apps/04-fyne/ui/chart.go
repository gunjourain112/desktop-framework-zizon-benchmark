package ui

import (
	"image/color"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	"apps/04-fyne/monitor"
)

// ChartWidget is a custom widget that renders a line chart.
type ChartWidget struct {
	widget.BaseWidget
	monitor *monitor.MonitorService
}

// NewChartWidget creates a new ChartWidget.
func NewChartWidget(m *monitor.MonitorService) *ChartWidget {
	c := &ChartWidget{monitor: m}
	c.ExtendBaseWidget(c)
	return c
}

// CreateRenderer implements fyne.Widget.
func (c *ChartWidget) CreateRenderer() fyne.WidgetRenderer {
	return &chartRenderer{
		chart: c,
		lines: make([]*canvas.Line, 0),
	}
}

type chartRenderer struct {
	chart      *ChartWidget
	lines      []*canvas.Line
	dataBuffer []float64
}

func (r *chartRenderer) Layout(size fyne.Size) {
	// Layout is handled in Refresh because we need to recalculate points based on size
}

func (r *chartRenderer) MinSize() fyne.Size {
	return fyne.NewSize(200, 150)
}

func (r *chartRenderer) Refresh() {
	// Re-generate lines based on history
	r.updateLines()
	canvas.Refresh(r.chart)
}

func (r *chartRenderer) BackgroundColor() color.Color {
	return theme.BackgroundColor()
}

func (r *chartRenderer) Objects() []fyne.CanvasObject {
	objs := make([]fyne.CanvasObject, len(r.lines))
	for i, l := range r.lines {
		objs[i] = l
	}
	return objs
}

func (r *chartRenderer) Destroy() {}

func (r *chartRenderer) updateLines() {
	// Reuse buffer
	if r.dataBuffer == nil {
		r.dataBuffer = make([]float64, 0, 100)
	}
	r.dataBuffer = r.dataBuffer[:0]

	// Get data
	r.chart.monitor.CPUHistory.Do(func(v float64) {
		r.dataBuffer = append(r.dataBuffer, v)
	})

	if len(r.dataBuffer) < 2 {
		r.lines = nil
		return
	}

	size := r.chart.Size()
	width := float32(size.Width)
	height := float32(size.Height)

	// Step per point
	step := width / float32(len(r.dataBuffer)-1)
	if step < 1 {
		step = 1 // Minimum 1 pixel step
	}

	// Ensure we have enough line objects
	numSegments := len(r.dataBuffer) - 1
	if len(r.lines) < numSegments {
		// Grow
		for i := len(r.lines); i < numSegments; i++ {
			line := canvas.NewLine(color.White) // Default color
			line.StrokeWidth = 2
			line.StrokeColor = theme.PrimaryColor()
			r.lines = append(r.lines, line)
		}
	} else if len(r.lines) > numSegments {
		// Shrink (slice)
		r.lines = r.lines[:numSegments]
	}

	// Update positions
	maxVal := 100.0 // CPU percent is 0-100

	for i := 0; i < numSegments; i++ {
		val1 := r.dataBuffer[i]
		val2 := r.dataBuffer[i+1]

		x1 := float32(i) * step
		y1 := height - (float32(val1)/float32(maxVal))*height

		x2 := float32(i+1) * step
		y2 := height - (float32(val2)/float32(maxVal))*height

		line := r.lines[i]
		line.Position1 = fyne.NewPos(x1, y1)
		line.Position2 = fyne.NewPos(x2, y2)
		// Re-apply color in case theme changed
		line.StrokeColor = theme.PrimaryColor()
	}
}
