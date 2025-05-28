import {Component, inject, signal} from '@angular/core';
import {Card} from 'primeng/card';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {InputMask} from 'primeng/inputmask';
import {FormBuilder, FormControl, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {TimelessApiService} from '../../timeless-api.service';
import {MessageService} from 'primeng/api';

@Component({
  selector: 'app-user-config',
  imports: [
    Card,
    InputText,
    Button,
    InputMask,
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './user-config.component.html',
  styleUrl: './user-config.component.scss'
})
export class UserConfigComponent {

  formBuilder = inject(FormBuilder)

  hasPhoneNumber = signal<boolean>(true)
  form = this.formBuilder.group({
    email: new FormControl({value: '', disabled: true}),
    firstName: new FormControl({value: '', disabled: true}),
    phoneNumber: [''],
    lastName: new FormControl({value: '', disabled: true})
  })

  constructor(private readonly timelessApiService: TimelessApiService, private readonly messageService: MessageService) {
    this.timelessApiService.userInfo()
      .subscribe((response: any) => {
        this.hasPhoneNumber.set(response.hasPhoneNumber)
        this.form.patchValue({
          email: response.email,
          firstName: response.firstName,
          lastName: response.lastName,
          phoneNumber: response.phoneNumber
        })
      })
  }


  update() {
    const phoneNumber = this.form.value.phoneNumber;

    if (!phoneNumber) {
      return
    }

    this.timelessApiService.updatePhoneNumber(phoneNumber)
      .subscribe(_ => {
        this.messageService.add({
          key: "toast",
          severity: "success",
          summary: "Tudo certo!",
          detail: "Seus dados foram atualizados"
        })
      });
  }
}

interface UserInfoResponse {
  id: string
  email: string
  firstName: string
  lastName: string
  phoneNumber: string
  hasPhoneNumber: boolean
}
