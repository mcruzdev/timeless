import { Component, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { Router, RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { TimelessApiService } from '../../../timeless-api.service';
import { FloatLabel } from 'primeng/floatlabel';
import { MessageService } from 'primeng/api';
import { timelessLocalStorageKey } from '../../../constants';

@Component({
  selector: 'app-sign-in',
  imports: [
    InputTextModule,
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    RouterLink,
    FloatLabel,
  ],
  templateUrl: './sign-in.component.html',
  styleUrl: './sign-in.component.scss',
  providers: [MessageService],
})
export class SignInComponent {
  private formBuilder = inject(FormBuilder);

  constructor(
    private readonly timelessApiService: TimelessApiService,
    private readonly router: Router,
    private readonly messageService: MessageService,
  ) {}

  form = this.formBuilder.group({
    email: ['', [Validators.email, Validators.required]],
    password: ['', [Validators.minLength(6), Validators.required]],
  });

  onSubmit() {
    if (this.form.invalid) {
      this.messageService.add({
        key: 'toast',
        severity: 'error',
        summary: 'Algo está errado',
        detail: 'O formulário de login se encontra inválido',
      });
      return;
    }

    this.timelessApiService.signIn(this.form.value).subscribe((value) => {
      localStorage.setItem(timelessLocalStorageKey, JSON.stringify(value));
      this.router.navigate(['/home']).then((r) => {
        console.log('logged in');
      });
    });
  }
}
