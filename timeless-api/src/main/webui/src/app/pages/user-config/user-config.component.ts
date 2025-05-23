import {Component, inject, signal} from '@angular/core';
import {Card} from 'primeng/card';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {InputMask} from 'primeng/inputmask';
import {FormsModule} from '@angular/forms';
import {TimelessApiService} from '../../timeless-api.service';
import {MessageService} from 'primeng/api';

@Component({
  selector: 'app-user-config',
  imports: [
    Card,
    InputText,
    Button,
    InputMask,
    FormsModule
  ],
  templateUrl: './user-config.component.html',
  styleUrl: './user-config.component.scss'
})
export class UserConfigComponent {

  userInfo = signal<UserInfoResponse>({
    id: '',
    email: '',
    firstName: '',
    phoneNumber: '',
    lastName: '',
    hasPhoneNumber: true
  })

  constructor(private readonly timelessApiService: TimelessApiService, private readonly messageService: MessageService) {
    this.timelessApiService.userInfo()
      .subscribe((response: any) => {
        this.userInfo.update(value => ({...response}))
      })
  }


  update() {
    this.timelessApiService.updatePhoneNumber(this.userInfo().phoneNumber)
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
