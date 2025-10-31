import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';
import { routes } from './app.routes';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/interceptors/auth.interceptor'; // Đảm bảo đường dẫn đúng

// --- BẮT ĐẦU THÊM LOCALE ---
import { registerLocaleData } from '@angular/common';
import localeVi from '@angular/common/locales/vi'; // Import dữ liệu tiếng Việt

// Đăng ký locale tiếng Việt
registerLocaleData(localeVi); 
// --- KẾT THÚC THÊM LOCALE ---

export const appConfig: ApplicationConfig = {
  providers: [
    // Bỏ provideBrowserGlobalErrorListeners() vì nó không còn được khuyến nghị
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])), // Sử dụng interceptor hàm
    provideAnimations(),
    provideToastr({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
    }),
    // Bạn có thể cần thêm LOCALE_ID nếu muốn đặt locale mặc định toàn ứng dụng
    // { provide: LOCALE_ID, useValue: 'vi-VN' } // Import LOCALE_ID from '@angular/core'
  ]
};

