import { HttpInterceptorFn } from '@angular/common/http';
import { timelessLocalStorageKey } from './constants';

const allowedPaths = [
  '/api/sign-in',
  '/api/sign-up',
  'api/sign-in',
  'api/sign-up',
];

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const path = req.url;

  if (allowedPaths.some((allowedPath) => path.startsWith(allowedPath))) {
    return next(req);
  }

  const data = localStorage.getItem(timelessLocalStorageKey);

  if (data == null) {
    return next(req);
  }

  const user = JSON.parse(data);

  if (user.token) {
    const clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${user.token}`,
      },
    });
    return next(clonedReq);
  }

  return next(req);
};
