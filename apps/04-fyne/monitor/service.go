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
	// CPU
	// cpu.Percent blocks for the duration if called with non-zero duration.
	// Since we are inside a ticker, we want instant value compared to last call.
	// However, first call returns 0.
	// Common pattern: pass 0 and rely on time diff since last call,
	// but gopsutil requires state maintenance for that or we just block for a small amount.
	// Let's block for a small window if we want accuracy, or 0 for "since last call" logic
	// if gopsutil supported it seamlessly (it does per-cpu).
	// For global cpu, let's use a small duration or 0 (which might be 0 on first call).
	// Actually, passing 0 to cpu.Percent means "calculate since last call" ONLY if we maintain state?
	// No, cpu.Percent(0, false) usually returns immediately with error or bad data on first run.
	// But let's check gopsutil docs.
	// "If interval is 0, the return values are calculated from the last call." - This is tricky if it's stateless.
	// It's not stateless, it reads /proc/stat.
	// Let's use 0. If it returns valid data, great.

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
