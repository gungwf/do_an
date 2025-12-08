import { ApplicationConfig, provideZoneChangeDetection, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';
import { routes } from './app.routes';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/interceptors/auth.interceptor';

// ===== LOCALE SETUP =====
import { registerLocaleData } from '@angular/common';
import localeVi from '@angular/common/locales/vi';

// Đăng ký locale tiếng Việt
registerLocaleData(localeVi);

export const appConfig: ApplicationConfig = {
  providers: [
    // ✅ Zone Change Detection với optimization
    provideZoneChangeDetection({ 
      eventCoalescing: true,
      runCoalescing: true 
    }),
    
    // ✅ Router
    provideRouter(routes),
    
    // ✅ HTTP Client với Fetch API và Auth Interceptor
    provideHttpClient(
      withFetch(), 
      withInterceptors([authInterceptor])
    ),
    
    // ✅ Animations (required for Toastr)
    provideAnimations(),
    
    // ✅ Toastr với cấu hình tối ưu
    provideToastr({
      timeOut: 2500,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      progressBar: true,
      closeButton: true,
      newestOnTop: true,
      tapToDismiss: true,
      maxOpened: 3,
      autoDismiss: true,
      enableHtml: false,
      easeTime: 300,
      easing: 'ease-in',
      extendedTimeOut: 1500,
      toastClass: 'ngx-toastr',
      titleClass: 'toast-title',
      messageClass: 'toast-message',
      iconClasses: {
        error: 'toast-error',
        info: 'toast-info',
        success: 'toast-success',
        warning: 'toast-warning'
      }
    }),
    
    // ✅ Set Vietnamese as default locale
    { 
      provide: LOCALE_ID, 
      useValue: 'vi-VN' 
    }
  ]
};