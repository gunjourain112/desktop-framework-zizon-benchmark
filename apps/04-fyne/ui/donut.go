package ui

import (
	"image/color"
	"math"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	"apps/04-fyne/monitor"
)

// DonutWidget renders a memory usage donut chart using vector lines (canvas.Line).
// This avoids per-frame image allocation and uses native drawing primitives.
type DonutWidget struct {
	widget.BaseWidget
	monitor *monitor.MonitorService
}

// NewDonutWidget creates a new DonutWidget.
func NewDonutWidget(m *monitor.MonitorService) *DonutWidget {
	d := &DonutWidget{monitor: m}
	d.ExtendBaseWidget(d)
	return d
}

// CreateRenderer implements fyne.Widget.
func (d *DonutWidget) CreateRenderer() fyne.WidgetRenderer {
	return &donutRenderer{
		donut:    d,
		segments: make([]*canvas.Line, 0),
		bgCircle: canvas.NewCircle(theme.DisabledColor()),
	}
}

type donutRenderer struct {
	donut    *DonutWidget
	segments []*canvas.Line // Active segments for the arc
	bgCircle *canvas.Circle // Background circle (grey)
}

func (r *donutRenderer) Layout(size fyne.Size) {
	// Layout logic handled in Refresh to support dynamic resizing/redrawing
}

func (r *donutRenderer) MinSize() fyne.Size {
	return fyne.NewSize(150, 150)
}

func (r *donutRenderer) Refresh() {
	r.updateArc()
	canvas.Refresh(r.donut)
}

func (r *donutRenderer) BackgroundColor() color.Color {
	return theme.BackgroundColor()
}

func (r *donutRenderer) Objects() []fyne.CanvasObject {
	// Return background circle + segments
	objs := make([]fyne.CanvasObject, 0, len(r.segments)+1)
	// Actually, we don't want a filled background circle, we want a "track".
	// Since Fyne Circle can check StrokeWidth, let's use that for background track.
	objs = append(objs, r.bgCircle)
	for _, l := range r.segments {
		objs = append(objs, l)
	}
	return objs
}

func (r *donutRenderer) Destroy() {}

func (r *donutRenderer) updateArc() {
	size := r.donut.Size()
	w, h := float64(size.Width), float64(size.Height)
	cx, cy := w/2, h/2
	radius := math.Min(w, h)/2 - 10 // Padding
	if radius < 1 {
		return
	}

	// Update Background Circle (Track)
	r.bgCircle.StrokeColor = theme.DisabledColor()
	r.bgCircle.StrokeWidth = 15
	r.bgCircle.FillColor = color.Transparent
	r.bgCircle.Resize(fyne.NewSize(float32(radius*2), float32(radius*2)))
	r.bgCircle.Move(fyne.NewPos(float32(cx-radius), float32(cy-radius)))

	// Calculate Arc
	usage := r.donut.monitor.Stats.MemoryUsage
	if usage > 100 {
		usage = 100
	}

	// Total angle 360 degrees (2Pi)
	totalAngle := (usage / 100.0) * 2 * math.Pi
	startAngle := -math.Pi / 2 // Top

	// Number of segments for smoothness
	// 60 segments for full circle is decent? Maybe 100.
	// Let's say 1 segment per 3 degrees -> 120 segments max.
	const maxSegments = 100
	segmentsNeeded := int((usage / 100.0) * maxSegments)
	if segmentsNeeded < 1 && usage > 0 {
		segmentsNeeded = 1
	}

	// Early return if no segments needed to avoid division by zero
	if segmentsNeeded == 0 {
		r.segments = r.segments[:0]
		return
	}

	// Ensure slice capacity
	if len(r.segments) < segmentsNeeded {
		for i := len(r.segments); i < segmentsNeeded; i++ {
			l := canvas.NewLine(theme.PrimaryColor())
			l.StrokeWidth = 15
			r.segments = append(r.segments, l)
		}
	} else if len(r.segments) > segmentsNeeded {
		r.segments = r.segments[:segmentsNeeded]
	}

	// Position segments
	stepAngle := totalAngle / float64(segmentsNeeded)
	currentAngle := startAngle

	primary := theme.PrimaryColor()

	for i := 0; i < segmentsNeeded; i++ {
		l := r.segments[i]
		l.StrokeColor = primary
		l.StrokeWidth = 15

		// Segment from currentAngle to currentAngle + stepAngle
		// To make it look like a smooth curve with lines, the lines must be short.
		// Approximating an arc with chords.

		x1 := cx + radius*math.Cos(currentAngle)
		y1 := cy + radius*math.Sin(currentAngle)

		nextAngle := currentAngle + stepAngle
		x2 := cx + radius*math.Cos(nextAngle)
		y2 := cy + radius*math.Sin(nextAngle)

		l.Position1 = fyne.NewPos(float32(x1), float32(y1))
		l.Position2 = fyne.NewPos(float32(x2), float32(y2))

		currentAngle = nextAngle
	}
}
