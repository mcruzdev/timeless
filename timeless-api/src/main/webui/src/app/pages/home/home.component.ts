import {Component} from '@angular/core';
import {ToolbarModule} from 'primeng/toolbar';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {AvatarModule} from 'primeng/avatar';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [ToolbarModule, CardModule, ButtonModule, AvatarModule, TableModule, TagModule, CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

  records = [
    {
      amount: 10.00,
      description: 'Chocolate',
      type: 'OUT',
      when: '10/05/2025'
    },
    {
      amount: 20.00,
      description: 'Pix da dona Lúcia',
      type: 'IN',
      when: '09/05/2025'
    }
  ].map(item => ({
    ...item,
    tag: item.type === 'OUT' ? 'Saída' : 'Entrada',
    icon: item.type === 'OUT' ? 'pi pi-arrow-circle-down' : 'pi pi-arrow-circle-up'
  }))

  balance = this.records.map(item => {
    return item.type === 'OUT' ? item.amount * -1 : item.amount
  }).reduce((prev, curr) => prev + curr)

  eyes = true

  changeEyes() {
    this.eyes = !this.eyes
  }
}
