#![cfg_attr(
    all(not(debug_assertions), target_os = "windows"),
    windows_subsystem = "windows"
)]

use std::thread;
use std::time::Duration;
use tauri::{Manager, Emitter};
use sysinfo::{CpuRefreshKind, RefreshKind, System, MemoryRefreshKind};
use serde::Serialize;

#[derive(Serialize, Clone)]
struct CpuData {
    currentLoad: f32,
}

#[derive(Serialize, Clone)]
struct MemoryData {
    total: u64,
    used: u64,
    free: u64,
    available: u64,
}

#[derive(Serialize, Clone)]
struct ProcessData {
    platform: String,
    cpuModel: String,
    node: String,
    electron: String,
    chrome: String,
}

#[derive(Serialize, Clone)]
struct SystemStats {
    cpu: CpuData,
    memory: MemoryData,
    process: ProcessData,
}

pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            let app_handle = app.handle().clone();

            thread::spawn(move || {
                let mut sys = System::new_with_specifics(
                    RefreshKind::new()
                        .with_cpu(CpuRefreshKind::everything())
                        .with_memory(MemoryRefreshKind::everything()),
                );

                // Wait a bit for CPU usage to be collected correctly (first call often returns 0)
                std::thread::sleep(sysinfo::MINIMUM_CPU_UPDATE_INTERVAL);
                sys.refresh_cpu_all();

                loop {
                    sys.refresh_cpu_all();
                    sys.refresh_memory();

                    let cpu_usage = sys.global_cpu_info().cpu_usage();
                    let total_memory = sys.total_memory();
                    let used_memory = sys.used_memory();
                    let free_memory = sys.free_memory();
                    let available_memory = sys.available_memory();

                    let cpu_brand = sys.cpus().first().map(|cpu| cpu.brand().to_string()).unwrap_or_else(|| "Unknown".to_string());
                    let os_name = System::name().unwrap_or_else(|| "Unknown".to_string());
                    let os_version = System::os_version().unwrap_or_else(|| "Unknown".to_string());
                    let platform = format!("{} {}", os_name, os_version);

                    let payload = SystemStats {
                        cpu: CpuData {
                            currentLoad: cpu_usage,
                        },
                        memory: MemoryData {
                            total: total_memory,
                            used: used_memory,
                            free: free_memory,
                            available: available_memory,
                        },
                        process: ProcessData {
                            platform,
                            cpuModel: cpu_brand,
                            node: "N/A (Rust Backend)".to_string(),
                            electron: "Tauri v2".to_string(),
                            chrome: "WebView".to_string(),
                        },
                    };

                    // Emit event to frontend
                    if let Err(e) = app_handle.emit("system-stats", &payload) {
                        eprintln!("Failed to emit system-stats: {}", e);
                    }

                    thread::sleep(Duration::from_secs(1));
                }
            });

            Ok(())
        })
        .plugin(tauri_plugin_shell::init())
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
