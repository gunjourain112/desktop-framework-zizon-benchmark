package monitor

import (
	"time"

	"github.com/shirou/gopsutil/v4/cpu"
	"github.com/shirou/gopsutil/v4/mem"
)

// SystemStats holds current system statistics.
// Optimized for memory alignment:
// float64 (8 bytes), uint64 (8 bytes) -> no padding needed.
type SystemStats struct {
	CPUUsage    float64 // Last CPU percent
	MemoryUsage float64 // Memory used percent
	MemoryTotal uint64  // Total memory in bytes
	MemoryUsed  uint64  // Used memory in bytes
}

// MonitorService handles system stats collection.
type MonitorService struct {
	CPUHistory *RingBuffer
	Stats      SystemStats
	done       chan struct{}
}

// NewMonitorService creates a new monitor.
func NewMonitorService(historySize int) *MonitorService {
	return &MonitorService{
		CPUHistory: NewRingBuffer(historySize),
		done:       make(chan struct{}),
	}
}

// Start begins data collection in a background goroutine.
// It calls the onUpdate callback whenever stats are updated.
func (s *MonitorService) Start(interval time.Duration, onUpdate func()) {
	go func() {
		ticker := time.NewTicker(interval)
		defer ticker.Stop()

		for {
			select {
			case <-s.done:
				return
			case <-ticker.C:
				s.updateStats()
				if onUpdate != nil {
					onUpdate()
				}
			}
		}
	}()
}

// Stop stops the background collection.
func (s *MonitorService) Stop() {
	close(s.done)
}

func (s *MonitorService) updateStats() {
	// Use 0 interval to calculate CPU usage since last call. First call may return 0.
	percent, err := cpu.Percent(0, false)
	if err == nil && len(percent) > 0 {
		val := percent[0]
		s.Stats.CPUUsage = val
		s.CPUHistory.Add(val)
	}

	// Memory
	v, err := mem.VirtualMemory()
	if err == nil {
		s.Stats.MemoryTotal = v.Total
		s.Stats.MemoryUsed = v.Used
		s.Stats.MemoryUsage = v.UsedPercent
	}
}
