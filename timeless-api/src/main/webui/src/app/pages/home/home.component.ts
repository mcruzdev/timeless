import {Component} from '@angular/core';
import {ToolbarModule} from 'primeng/toolbar';
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {AvatarModule} from 'primeng/avatar';
import {TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {RecordResponseItem, TimelessApiService} from '../../timeless-api.service';

@Component({
  selector: 'app-home',
  imports: [ToolbarModule, CardModule, ButtonModule, AvatarModule, TableModule, TagModule, CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

  records: RecordResponseItem[] = []
  balance = 0
  eyes = true

  constructor(private readonly timelessApiService: TimelessApiService) {

    this.timelessApiService.getRecords().subscribe(body => {
      if (body.length) {
        this.records = body.map(item => ({
          ...item,
          tag: item.recordType === 'OUT' ? 'Saída' : 'Entrada',
          icon: item.recordType === 'OUT' ? 'pi pi-arrow-circle-up' : 'pi pi-arrow-circle-down'
        }))
        this.balance = this.records.map(item => {
          return item.recordType === 'OUT' ? item.amount * -1 : item.amount
        }).reduce((prev, curr) => prev + curr)
      }
    })
  }

  changeEyes() {
    this.eyes = !this.eyes
  }
}
