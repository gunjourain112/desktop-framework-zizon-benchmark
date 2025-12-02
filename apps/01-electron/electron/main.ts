import { app, BrowserWindow, ipcMain } from 'electron';
import * as path from 'path';
import * as si from 'systeminformation';

// 개발 모드 여부 확인 (환경변수 또는 실행 인자 등)
const isDev = process.env.npm_lifecycle_event === 'electron:wait' || process.env.NODE_ENV === 'development';

let mainWindow: BrowserWindow | null = null;
let updateInterval: NodeJS.Timeout | null = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1024,
    height: 768,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
    },
    title: 'System Monitor',
  });

  if (isDev) {
    mainWindow.loadURL('http://localhost:5173');
    mainWindow.webContents.openDevTools();
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
  }

  // 윈도우 닫힐 때 처리
  mainWindow.on('closed', () => {
    mainWindow = null;
    if (updateInterval) {
      clearInterval(updateInterval);
      updateInterval = null;
    }
  });

  // 데이터 수집 시작
  startSystemMonitoring();
}

function startSystemMonitoring() {
  if (updateInterval) clearInterval(updateInterval);

  updateInterval = setInterval(async () => {
    if (!mainWindow) return;

    try {
      const cpuLoad = await si.currentLoad();
      const memory = await si.mem();
      const cpuInfo = await si.cpu();

      const data = {
        cpu: {
          currentLoad: cpuLoad.currentLoad,
          history: [] as number[] // Frontend에서 관리하거나 필요시 여기서 누적
        },
        memory: {
          total: memory.total,
          used: memory.used,
          free: memory.free,
          active: memory.active,
          available: memory.available
        },
        process: {
            node: process.version,
            electron: process.versions.electron,
            chrome: process.versions.chrome,
            platform: process.platform,
            cpuModel: cpuInfo.brand
        }
      };

      mainWindow.webContents.send('system-update', data);
    } catch (error) {
      console.error('Error fetching system info:', error);
    }
  }, 1000);
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
