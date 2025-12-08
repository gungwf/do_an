import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// ✅ FIX: Polyfill 'global', 'process', 'Buffer', 'module' for browser
(window as any).global = window;
(window as any).process = {
  env: {},
  version: '',
  nextTick: (fn: Function) => setTimeout(fn, 0),
  browser: true
};
(window as any).Buffer = {
  isBuffer: () => false,
  from: (data: any) => data,
  alloc: (size: number) => new Uint8Array(size)
};
(window as any).module = { exports: {} };
(window as any).exports = {};

// ✅ CLEANUP CORRUPTED DATA ON STARTUP
function cleanupStorage(): void {
  try {
    console.log('🧹 Checking localStorage...');
    
    const keysToValidate = ['shopping_cart', 'user_data', 'access_token'];
    
    keysToValidate.forEach(key => {
      const value = localStorage.getItem(key);
      if (value) {
        try {
          JSON.parse(value);
          console.log(`✅ ${key} is valid`);
        } catch (error) {
          console.warn(`🗑️ Removing corrupted: ${key}`);
          localStorage.removeItem(key);
        }
      }
    });
    
    console.log('✅ Storage cleanup complete');
  } catch (error) {
    console.error('❌ Storage cleanup error:', error);
    // If major error, clear all
    localStorage.clear();
  }
}

// Run cleanup BEFORE bootstrap
cleanupStorage();

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
