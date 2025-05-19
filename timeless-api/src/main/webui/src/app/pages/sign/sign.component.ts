import {Component} from '@angular/core';
import {InputTextModule} from 'primeng/inputtext';
import {FormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {TimelessApiService} from '../../timeless-api.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-sign',
  imports: [InputTextModule, FormsModule, ButtonModule],
  templateUrl: './sign.component.html',
  styleUrl: './sign.component.scss'
})
export class SignComponent {

  constructor(private readonly timelessApiService: TimelessApiService, private readonly router: Router) {
  }

  email: string = 'john.doe@email.com'
  password: string = 'jhondoe@123'

  onSubmit() {
    this.timelessApiService.signIn(this.email, this.password)
      .subscribe(value => {
        localStorage.setItem("timeless-api::token", value.token)
        this.router.navigate(['/home'])
      });
  }
}
