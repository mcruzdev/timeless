import { Component, inject, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { CurrencyPipe } from '@angular/common';
import {
  RecordResponseItem,
  TimelessApiService,
} from '../../timeless-api.service';
import { Paginator, PaginatorState } from 'primeng/paginator';
import { Card } from 'primeng/card';
import { Button } from 'primeng/button';

@Component({
  selector: 'app-records',
  imports: [TableModule, Tag, CurrencyPipe, Paginator, Card, Button],
  templateUrl: './records.component.html',
  styleUrl: './records.component.scss',
})
export class RecordsComponent {
  eyes = signal(true);
  balance = signal(0);
  records: RecordResponseItem[] = [];
  timelessApiService = inject(TimelessApiService);
  first = signal<number>(0);
  rows = signal<number>(10);
  totalRecords = signal<number>(0);
  totalIn = signal<number>(0);
  totalExpenses = signal<number>(0);

  constructor() {
    this.populatePaginator();
  }

  private populatePaginator() {
    this.timelessApiService
      .getRecords(this.first(), this.rows())
      .subscribe((body) => {
        if (body.items.length > 0) {
          this.records = body.items.map((item) => ({
            ...item,
            tag: item.transaction === 'OUT' ? 'Saída' : 'Entrada',
            icon:
              item.transaction === 'OUT'
                ? 'pi pi-arrow-circle-up'
                : 'pi pi-arrow-circle-down',
          }));

          this.totalRecords.set(body.totalRecords);
          this.totalIn.set(body.totalIn);
          this.totalExpenses.set(body.totalExpenses);

          this.balance.set(this.totalIn() - this.totalExpenses());
        }
      });
  }

  changeEyes() {
    this.eyes.update((value) => !value);
  }

  onPageChange($event: PaginatorState) {
    this.first.set($event.page || 0);
    this.rows.set($event.rows || 10);
    this.populatePaginator();
  }

  deleteRecord(id: number) {
    this.timelessApiService.deleteRecord(id).subscribe(() => {
      this.populatePaginator();
    });
  }
}
