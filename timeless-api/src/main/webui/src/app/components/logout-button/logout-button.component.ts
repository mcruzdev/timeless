import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { TimelessApiService } from '../../timeless-api.service';
import { CommonModule } from '@angular/common';
import { EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-logout-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './logout-button.component.html',
  styleUrl: './logout-button.component.scss',
})
export class LogoutButtonComponent {
  @Input() textButton: string = '';
  @Input() disabled: boolean = true;
  constructor(
    private readonly timelessApiService: TimelessApiService,
    private readonly router: Router,
  ) {}

  onLogout() {
    if (this.disabled) return;

    this.timelessApiService.logout();
    this.router.navigate(['/']);
  }
}
