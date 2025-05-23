import {Component, inject, signal} from '@angular/core';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {CurrencyPipe} from '@angular/common';
import {RecordResponseItem, TimelessApiService} from '../../timeless-api.service';

@Component({
  selector: 'app-records',
  imports: [
    Button,
    TableModule,
    Tag,
    CurrencyPipe
  ],
  templateUrl: './records.component.html',
  styleUrl: './records.component.scss'
})
export class RecordsComponent {
  eyes = signal(true)
  balance = signal(0)
  records: RecordResponseItem[] = []
  timelessApiService = inject(TimelessApiService)

  constructor() {
    this.timelessApiService.getRecords().subscribe(body => {
      if (body.length > 0) {
        this.records = body.map(item => ({
          ...item,
          tag: item.recordType === 'OUT' ? 'Saída' : 'Entrada',
          icon: item.recordType === 'OUT' ? 'pi pi-arrow-circle-up' : 'pi pi-arrow-circle-down'
        }))

        this.records.map(item => {
          return item.recordType === 'OUT' ? item.amount * -1 : item.amount
        });
      }
    })
  }

  changeEyes() {
    this.eyes.update(value => !value)
  }
}
