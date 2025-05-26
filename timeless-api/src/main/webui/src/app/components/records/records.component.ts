import {Component, inject, signal} from '@angular/core';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {CurrencyPipe} from '@angular/common';
import {RecordResponseItem, TimelessApiService} from '../../timeless-api.service';
import {Paginator, PaginatorState} from 'primeng/paginator';

@Component({
  selector: 'app-records',
  imports: [
    Button,
    TableModule,
    Tag,
    CurrencyPipe,
    Paginator
  ],
  templateUrl: './records.component.html',
  styleUrl: './records.component.scss'
})
export class RecordsComponent {
  eyes = signal(true)
  balance = signal(0)
  records: RecordResponseItem[] = []
  timelessApiService = inject(TimelessApiService)
  first = signal<number>(0)
  rows = signal<number>(10)
  totalRecords = signal<number>(0)

  constructor() {
    this.populatePaginator();
  }

  private populatePaginator() {
    console.log(this.first(), this.rows())
    this.timelessApiService.getRecords(this.first(), this.rows()).subscribe(body => {
      if (body.items.length > 0) {
        this.records = body.items.map(item => ({
          ...item,
          tag: item.recordType === 'OUT' ? 'Saída' : 'Entrada',
          icon: item.recordType === 'OUT' ? 'pi pi-arrow-circle-up' : 'pi pi-arrow-circle-down'
        }))

        this.totalRecords.set(body.totalRecords)

        this.balance.set(this.records.map(item => {
          return item.recordType === 'OUT' ? item.amount * -1 : item.amount
        }).reduce((previousValue, currentValue) => (previousValue + currentValue)))
      }
    })
  }

  changeEyes() {
    this.eyes.update(value => !value)
  }

  onPageChange($event: PaginatorState) {
    this.first.set($event.page || 0)
    this.rows.set($event.rows || 10)
    this.populatePaginator()
  }
}
