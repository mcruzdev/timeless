import { Component, inject, signal } from '@angular/core';
import { Card } from 'primeng/card';
import { InputText } from 'primeng/inputtext';
import { ButtonDirective } from 'primeng/button';
import { InputMask } from 'primeng/inputmask';
import { TooltipModule } from 'primeng/tooltip';
import {
  FormBuilder,
  FormControl,
  FormsModule,
  ReactiveFormsModule,
} from '@angular/forms';
import { TimelessApiService } from '../../timeless-api.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-user-config',
  imports: [
    Card,
    InputText,
    InputMask,
    FormsModule,
    ReactiveFormsModule,
    ButtonDirective,
    TooltipModule,
  ],
  templateUrl: './user-config.component.html',
  styleUrl: './user-config.component.scss',
})
export class UserConfigComponent {
  formBuilder = inject(FormBuilder);

  hasPhoneNumber = signal<boolean>(true);
  isEditing = signal<boolean>(false);
  form = this.formBuilder.group({
    email: new FormControl({ value: '', disabled: true }),
    firstName: new FormControl({ value: '', disabled: true }),
    phoneNumber: [''],
    lastName: new FormControl({ value: '', disabled: true }),
  });

  constructor(
    private readonly timelessApiService: TimelessApiService,
    private readonly toast: ToastService,
  ) {
    this.timelessApiService.userInfo().subscribe((response: any) => {
      this.hasPhoneNumber.set(response.hasPhoneNumber);
      this.form.patchValue({
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        phoneNumber: response.phoneNumber,
      });
    });
  }

  toggleEdit() {
    this.isEditing.update((value) => !value);
    if (this.isEditing()) {
      this.form.enable();
    } else {
      this.form.disable();
    }
  }

  update() {
    if (this.form.invalid) {
      return;
    }

    const { firstName, lastName, email, phoneNumber } = this.form.getRawValue();

    this.timelessApiService
      .updateUser({
        id: '',
        firstName: firstName || '',
        lastName: lastName || '',
        email: email || '',
        phoneNumber: phoneNumber || '',
      })
      .subscribe((_) => {
        this.toast.success('Tudo certo!', 'Seus dados foram atualizados');
        this.isEditing.set(false);
        this.form.disable();
      });
  }
}

interface UserInfoResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  hasPhoneNumber: boolean;
}
