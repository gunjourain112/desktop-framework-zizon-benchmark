import { contextBridge, ipcRenderer } from 'electron';

interface SystemData {
  cpu: {
    currentLoad: number;
  };
  memory: {
    total: number;
    used: number;
    free: number;
    active: number;
    available: number;
  };
  process: {
    node: string;
    electron: string;
    chrome: string;
    platform: string;
    cpuModel: string;
  };
}

contextBridge.exposeInMainWorld('electron', {
  onSystemUpdate: (callback: (data: SystemData) => void) => {
    // 이벤트 리스너 래핑 (메모리 누수 방지 및 보안)
    const subscription = (_: any, data: SystemData) => callback(data);
    ipcRenderer.on('system-update', subscription);

    // 클린업 함수 반환 (React useEffect 등에서 사용 가능)
    return () => {
      ipcRenderer.removeListener('system-update', subscription);
    };
  },
});
