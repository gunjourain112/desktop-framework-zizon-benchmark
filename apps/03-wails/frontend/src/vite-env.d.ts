/// <reference types="vite/client" />

declare global {
  interface Window {
    runtime: {
      EventsOn(eventName: string, callback: (data: any) => void): void;
      EventsOff(eventName: string): void;
    };
  }
}

export {};
