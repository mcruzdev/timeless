import { Component, inject } from '@angular/core';
import { Button } from 'primeng/button';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { Router, RouterLink } from '@angular/router';
import { FloatLabel } from 'primeng/floatlabel';
import { TimelessApiService } from '../../../timeless-api.service';
import { catchError } from 'rxjs';
import { ToadService } from '../../../services/toad.service';
import { Toast } from 'primeng/toast';

@Component({
  selector: 'app-sign-up',
  imports: [
    Button,
    FormsModule,
    InputText,
    RouterLink,
    ReactiveFormsModule,
    FloatLabel,
    Toast,
  ],
  templateUrl: './sign-up.component.html',
  styleUrl: './sign-up.component.scss',
})
export class SignUpComponent {
  formBuilder = inject(FormBuilder);
  form: FormGroup = this.formBuilder.group({
    email: ['', [Validators.email, Validators.required]],
    password: [
      '',
      [Validators.minLength(8), Validators.maxLength(32), Validators.required],
    ],
    firstName: ['', [Validators.required, Validators.minLength(1)]],
    lastName: ['', [Validators.required, Validators.minLength(1)]],
  });

  constructor(
    private readonly timelessApiService: TimelessApiService,
    private readonly router: Router,
    private readonly toad: ToadService,
  ) {}

  onSubmit() {
    if (this.form.valid) {
      this.timelessApiService
        .signUp(this.form.value)
        .pipe(
          catchError((err: any) => {
            if (err.error.message) {
              this.toad.error('Conflito', err.error.message);
            }
            return err;
          }),
        )
        .subscribe((_) => {
          this.router.navigate(['/registered']);
        });
    } else {
      this.form.markAllAsTouched();
    }
  }
}
