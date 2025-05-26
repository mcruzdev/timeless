import {Component, signal} from '@angular/core';
import {ToolbarModule} from 'primeng/toolbar';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {AvatarModule} from 'primeng/avatar';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {TimelessApiService} from '../../timeless-api.service';
import {timelessLocalStorageKey} from '../../constants';
import {Menubar} from 'primeng/menubar';
import {MenuItem, PrimeIcons} from 'primeng/api';
import {Message} from 'primeng/message';

@Component({
  selector: 'app-home',
  imports: [ToolbarModule, CardModule, ButtonModule, AvatarModule, TableModule, TagModule, CommonModule, RouterModule, Menubar, Message],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

  hasPhoneNumber = signal(true)

  items: MenuItem[] = [
    {
      id: 'transactions',
      label: 'Gastos',
      styleClass: 'text-sm',
      icon: PrimeIcons.DOLLAR,
      routerLink: '/home'
    }
  ]

  constructor(private readonly router: Router, private readonly timelessApiService: TimelessApiService) {
    const data = localStorage.getItem(timelessLocalStorageKey);
    if (data == null) {
      this.router.navigate([''])
      return
    }
    this.timelessApiService.userInfo()
      .subscribe((response: any) => {
        this.hasPhoneNumber.set(response.hasPhoneNumber)
      })
  }
}
