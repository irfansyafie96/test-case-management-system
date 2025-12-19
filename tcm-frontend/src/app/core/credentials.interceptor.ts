import { HttpInterceptorFn } from '@angular/common/http';

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  // Clone the request and add withCredentials for all requests
  const authReq = req.clone({
    withCredentials: true
  });
  
  return next(authReq);
};