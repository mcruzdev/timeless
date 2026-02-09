import { Component, inject, signal, HostListener } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { CurrencyPipe, CommonModule } from '@angular/common';
import {
  RecordResponseItem,
  TimelessApiService,
  UpdateRecord,
} from '../../timeless-api.service';
import { Paginator, PaginatorState } from 'primeng/paginator';
import { Card } from 'primeng/card';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-records',
  imports: [
    TableModule,
    Tag,
    CurrencyPipe,
    Paginator,
    Card,
    Button,
    Dialog,
    InputText,
    InputNumber,
    Select,
    DatePicker,
    ReactiveFormsModule,
    CommonModule,
    Toast,
  ],
  providers: [MessageService],
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
  hideTag = signal(false);
  isMobile = signal(false);
  editDialogVisible = signal(false);
  selectedRecord = signal<RecordResponseItem | null>(null);

  private fb = inject(FormBuilder);
  private messageService = inject(MessageService);
  editForm: FormGroup = this.fb.group({
    amount: [0, [Validators.required, Validators.min(0)]],
    description: ['', [Validators.required]],
    transaction: ['OUT', [Validators.required]],
    category: ['GENERAL', [Validators.required]],
    date: [new Date(), [Validators.required]],
  });

  categories = [
    { label: 'Custos Fixos', value: 'FIXED_COSTS' },
    { label: 'Lazer', value: 'PLEASURES' },
    { label: 'Conhecimento', value: 'KNOWLEDGE' },
    { label: 'Metas', value: 'GOALS' },
    { label: 'Conforto', value: 'COMFORT' },
    { label: 'Liberdade Financeira', value: 'FINANCIAL_FREEDOM' },
    { label: 'Geral', value: 'GENERAL' },
    { label: 'Nenhum', value: 'NONE' },
  ];

  transactions = [
    { label: 'Entrada', value: 'IN' },
    { label: 'Saída', value: 'OUT' },
  ];

  constructor() {
    this.checkScreenSize();
    this.populatePaginator();
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: any) {
    this.checkScreenSize();
  }

  private checkScreenSize() {
    this.isMobile.set(window.innerWidth <= 1280);
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
                ? 'pi pi-arrow-circle-down'
                : 'pi pi-arrow-circle-up',
          }));

          this.totalRecords.set(body.totalRecords);
          this.totalIn.set(body.totalIn);
          this.totalExpenses.set(body.totalExpenses);

          this.balance.set(this.totalIn() - this.totalExpenses());
        }
      });
  }

  showEditDialog(record: RecordResponseItem) {
    this.selectedRecord.set(record);
    const dateParts = record.date.split('/');
    const dateObj = new Date(+dateParts[2], +dateParts[1] - 1, +dateParts[0]);

    this.editForm.patchValue({
      amount: record.amount,
      description: record.description,
      transaction: record.transaction,
      category: record.category,
      date: dateObj,
    });
    this.editDialogVisible.set(true);
  }

  saveEdit() {
    if (this.editForm.valid && this.selectedRecord()) {
      const formValue = this.editForm.value;
      const formattedDate = formValue.date.toISOString().split('T')[0];

      const updateRequest: UpdateRecord = {
        ...formValue,
        transactionDate: formattedDate,
      };

      this.timelessApiService
        .updateRecord(this.selectedRecord()!.id, updateRequest)
        .subscribe({
          next: () => {
            this.editDialogVisible.set(false);
            this.populatePaginator();
            this.messageService.add({
              severity: 'success',
              summary: 'Sucesso',
              detail: 'Registro atualizado com sucesso!',
              life: 3000,
            });
          },
          error: (error) => {
            console.error('Error updating record:', error);
            this.messageService.add({
              severity: 'error',
              summary: 'Erro',
              detail: 'Falha ao atualizar registro.',
              life: 3000,
            });
          },
        });
    }
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
    this.timelessApiService.deleteRecord(id).subscribe({
      next: () => {
        this.populatePaginator();
        this.messageService.add({
          severity: 'success',
          summary: 'Sucesso',
          detail: 'Registro excluído com sucesso!',
          life: 3000,
        });
      },
      error: (error) => {
        console.error('Error deleting record:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erro',
          detail: 'Falha ao excluir registro.',
          life: 3000,
        });
      },
    });
  }
}
