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
import { catchError, throwError } from 'rxjs';
import { Toast } from 'primeng/toast';

@Component({
  selector: 'app-sign-in',
  imports: [
    InputTextModule,
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    RouterLink,
    FloatLabel,
    Toast,
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
      console.log('Form is invalid');
      this.messageService.add({
        key: 'toast',
        severity: 'error',
        summary: 'Algo está errado',
        detail: 'O formulário de login se encontra inválido',
      });
      return;
    }

    this.timelessApiService
      .signIn(this.form.value)
      .pipe(
        catchError((error) => {
          console.error('Login error:', error);
          this.messageService.add({
            key: 'toast',
            severity: 'error',
            summary: 'Erro ao fazer login',
            detail:
              'Não foi possível fazer login, verifique suas credenciais e tente novamente.',
          });
          return throwError(() => error);
        }),
      )
      .subscribe((value) => {
        localStorage.setItem(timelessLocalStorageKey, JSON.stringify(value));
        this.router.navigate(['/home']).then((r) => {
          console.log('logged in');
        });
      });
  }
}
