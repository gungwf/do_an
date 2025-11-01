import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Interceptor này (dạng HÀM) tự động gắn Bearer Token (lấy từ localStorage)
 * vào header của mọi request gửi đi.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  
  // 1. Lấy token từ localStorage
  // (Sửa key 'token' thành 'healthcare_token' cho khớp với AuthService)
  const token = localStorage.getItem('healthcare_token');

  // 2. Nếu không có token, gửi request gốc
  if (!token) {
    return next(req);
  }

  // 3. Nếu có token, clone request và thêm header
  const clonedReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`, // Gắn Bearer Token
    },
  });

  // 4. Gửi request đã được clone đi
  return next(clonedReq);
};


