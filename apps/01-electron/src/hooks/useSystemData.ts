import { useState, useEffect } from 'react';

export interface SystemData {
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

// Preload script에서 노출한 API 타입 정의
interface ElectronAPI {
  onSystemUpdate: (callback: (data: SystemData) => void) => () => void;
}

declare global {
  interface Window {
    electron: ElectronAPI;
  }
}

export function useSystemData() {
  const [data, setData] = useState<SystemData | null>(null);

  useEffect(() => {
    if (window.electron && window.electron.onSystemUpdate) {
      // 구독 시작
      const cleanup = window.electron.onSystemUpdate((newData) => {
        setData(newData);
      });
      // 언마운트 시 구독 해제
      return cleanup;
    } else {
        // 브라우저에서 개발할 때를 위한 더미 데이터 (Optional)
        console.warn('Electron API not found. Running in browser mode?');
        const interval = setInterval(() => {
            setData({
                cpu: { currentLoad: Math.random() * 100 },
                memory: {
                    total: 16 * 1024 * 1024 * 1024,
                    used: 8 * 1024 * 1024 * 1024 + Math.random() * 1024 * 1024 * 1024,
                    free: 8 * 1024 * 1024 * 1024,
                    active: 4 * 1024 * 1024 * 1024,
                    available: 8 * 1024 * 1024 * 1024
                },
                process: {
                    node: 'Browser Mock',
                    electron: 'Browser Mock',
                    chrome: 'Browser Mock',
                    platform: 'Browser Mock',
                    cpuModel: 'Browser Mock CPU'
                }
            });
        }, 1000);
        return () => clearInterval(interval);
    }
  }, []);

  return data;
}
