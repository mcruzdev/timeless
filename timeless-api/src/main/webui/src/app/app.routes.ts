import {Routes} from '@angular/router';
import {SignComponent} from './pages/sign/sign.component';
import {HomeComponent} from './pages/home/home.component';

export const routes: Routes = [
  {
    path: '', pathMatch: 'full', component: SignComponent
  },
  {
    path: 'home', component: HomeComponent
  }
];
