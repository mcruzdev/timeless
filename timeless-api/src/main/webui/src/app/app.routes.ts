import { Routes } from '@angular/router';
import { SignComponent } from './pages/sign/sign.component';
import { HomeComponent } from './pages/home/home.component';
import { SignUpComponent } from './pages/sign/sign-up/sign-up.component';
import { SignInComponent } from './pages/sign/sign-in/sign-in.component';
import { RegisteredComponent } from './pages/sign/registered/registered.component';
import { UserConfigComponent } from './pages/user-config/user-config.component';
import { RecordsComponent } from './components/records/records.component';
import { AuthGuard } from './guards/auth_guard.component';
export const routes: Routes = [
  {
    path: '',
    component: SignComponent,
    children: [
      {
        path: '',
        component: SignInComponent,
      },
      {
        path: 'sign-up',
        component: SignUpComponent,
      },
      {
        path: 'registered',
        component: RegisteredComponent,
      },
    ],
  },
  {
    path: 'home',
    component: HomeComponent,
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    children: [
      {
        path: '',
        component: RecordsComponent,
      },
      {
        path: 'user-configs',
        component: UserConfigComponent,
      },
    ],
  },
];
