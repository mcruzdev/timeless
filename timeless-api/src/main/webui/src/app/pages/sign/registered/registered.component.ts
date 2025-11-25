import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-registered',
  imports: [FormsModule, ReactiveFormsModule],
  templateUrl: './registered.component.html',
  styleUrl: './registered.component.scss',
})
export class RegisteredComponent implements OnInit {
  time = signal(3);
  router = inject(Router);

  ngOnInit(): void {
    const interval = setInterval(() => {
      if (this.time() == 1) {
        this.router.navigate(['']).then(() => clearInterval(interval));
      } else {
        this.time.update((value) => value - 1);
      }
    }, 1000);
  }
}
