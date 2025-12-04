package main

import (
	"context"
	"fmt"
	"runtime"
	"time"

	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/mem"
	wailsRuntime "github.com/wailsapp/wails/v2/pkg/runtime"
)

// App struct
type App struct {
	ctx context.Context
}

// NewApp creates a new App application struct
func NewApp() *App {
	return &App{}
}

// startup is called when the app starts. The context is saved
// so we can call the runtime methods
func (a *App) startup(ctx context.Context) {
	a.ctx = ctx
	// Start system monitoring in a goroutine
	go a.monitorSystem()
}

// SystemData structures matching the frontend expectations
type CPUData struct {
	CurrentLoad float64 `json:"currentLoad"`
}

type MemoryData struct {
	Total     uint64 `json:"total"`
	Used      uint64 `json:"used"`
	Free      uint64 `json:"free"`
	Active    uint64 `json:"active"`
	Available uint64 `json:"available"`
}

type ProcessData struct {
	Node     string `json:"node"`
	Electron string `json:"electron"`
	Chrome   string `json:"chrome"`
	Platform string `json:"platform"`
	CPUModel string `json:"cpuModel"`
}

type SystemData struct {
	CPU     CPUData     `json:"cpu"`
	Memory  MemoryData  `json:"memory"`
	Process ProcessData `json:"process"`
}

func (a *App) monitorSystem() {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-a.ctx.Done():
			return
		case <-ticker.C:
			// Collect CPU Load
			percent, err := cpu.Percent(0, false)
			cpuLoad := 0.0
			if err == nil && len(percent) > 0 {
				cpuLoad = percent[0]
			}

			// Collect Memory Usage
			v, err := mem.VirtualMemory()
			memData := MemoryData{}
			if err == nil {
				memData = MemoryData{
					Total:     v.Total,
					Used:      v.Used,
					Free:      v.Free,
					Active:    v.Active,
					Available: v.Available,
				}
			}

			// Mock/Static Process Info
			// Note: gopsutil can get CPU info, but for "ProcessData" specifically matching Electron's "Node/Electron/Chrome"
			// we will provide Go/Wails info as requested.

			cpuInfo, _ := cpu.Info()
			cpuModel := "Unknown"
			if len(cpuInfo) > 0 {
				cpuModel = cpuInfo[0].ModelName
			}

			data := SystemData{
				CPU: CPUData{
					CurrentLoad: cpuLoad,
				},
				Memory: memData,
				Process: ProcessData{
					Node:     fmt.Sprintf("Go %s", runtime.Version()),
					Electron: "Wails v2",
					Chrome:   "WebView2/WebKit", // Generic for Wails
					Platform: runtime.GOOS,
					CPUModel: cpuModel,
				},
			}

			// Emit event to frontend
			wailsRuntime.EventsEmit(a.ctx, "system-update", data)
		}
	}
}
