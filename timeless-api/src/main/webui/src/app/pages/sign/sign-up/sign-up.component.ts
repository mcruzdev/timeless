import {Component, inject} from '@angular/core';
import {Button} from 'primeng/button';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {Router, RouterLink} from '@angular/router';
import {FloatLabel} from 'primeng/floatlabel';
import {TimelessApiService} from '../../../timeless-api.service';

@Component({
  selector: 'app-sign-up',
  imports: [
    Button,
    FormsModule,
    InputText,
    RouterLink,
    ReactiveFormsModule,
    FloatLabel,
  ],
  templateUrl: './sign-up.component.html',
  styleUrl: './sign-up.component.scss'
})
export class SignUpComponent {

  formBuilder = inject(FormBuilder)
  form: FormGroup = this.formBuilder.group({
    email: ['', [Validators.email, Validators.required]],
    password: ['', [Validators.minLength(6), Validators.required]],
    firstName: ['', [Validators.required, Validators.minLength(1)]],
    lastName: ['', [Validators.required, Validators.minLength(1)]]
  })

  constructor(private readonly timelessApiService: TimelessApiService, private readonly  router: Router) {
  }

  onSubmit() {
    if (this.form.valid) {
      this.timelessApiService.signUp(this.form.value)
        .subscribe((_) => {
            this.router.navigate(['/registered'])
        })
    } else {
      this.form.markAllAsTouched();
    }
  }

}
