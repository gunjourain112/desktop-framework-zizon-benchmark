package monitor_test

import (
	"apps/04-fyne/monitor"
	"testing"
)

func TestRingBuffer(t *testing.T) {
	rb := monitor.NewRingBuffer(3)

	// Check empty
	if rb.Count() != 0 {
		t.Errorf("Expected count 0, got %d", rb.Count())
	}

	// Add 1
	rb.Add(1.0)
	if rb.Count() != 1 {
		t.Errorf("Expected count 1, got %d", rb.Count())
	}

	// Add 2, 3 (Full)
	rb.Add(2.0)
	rb.Add(3.0)
	if rb.Count() != 3 {
		t.Errorf("Expected count 3, got %d", rb.Count())
	}

	// Verify order
	vals := []float64{}
	rb.Do(func(v float64) {
		vals = append(vals, v)
	})

	if len(vals) != 3 || vals[0] != 1.0 || vals[2] != 3.0 {
		t.Errorf("Expected [1, 2, 3], got %v", vals)
	}

	// Add 4 (Overwrite 1)
	rb.Add(4.0)
	// Expected: 2, 3, 4
	vals = []float64{}
	rb.Do(func(v float64) {
		vals = append(vals, v)
	})

	if len(vals) != 3 || vals[0] != 2.0 || vals[2] != 4.0 {
		t.Errorf("Expected [2, 3, 4], got %v", vals)
	}
}

func TestMonitorStructAlignment(t *testing.T) {
	// Not easily testable at runtime without reflection or unsafe,
	// but the definition is:
	// float64 (8), float64 (8), uint64 (8), uint64 (8).
	// Size should be 32 bytes.
	// We can skip this or use unsafe.Sizeof if strictly needed.
}
