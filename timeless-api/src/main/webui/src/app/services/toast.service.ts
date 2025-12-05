import { Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private readonly defaultKey = 'toast';

  constructor(private readonly messageService: MessageService) {}

  show(message: ToastMessage) {
    this.messageService.add({
      key: this.defaultKey,
      ...message,
    });
  }

  success(summary: string, detail?: string, life = 3000) {
    this.show({
      severity: 'success',
      summary,
      detail,
      life,
    });
  }

  error(summary: string, detail?: string, life = 5000) {
    this.show({
      severity: 'error',
      summary,
      detail,
      life,
    });
  }

  info(summary: string, detail?: string, life = 3000) {
    this.show({
      severity: 'info',
      summary,
      detail,
      life,
    });
  }

  warn(summary: string, detail?: string, life = 4000) {
    this.show({
      severity: 'warn',
      summary,
      detail,
      life,
    });
  }

  clearAll() {
    this.messageService.clear();
  }
}

export interface ToastMessage {
  key?: string;
  severity?: 'success' | 'info' | 'warn' | 'error' | string;
  summary?: string;
  detail?: string;
  life?: number;
  sticky?: boolean;
  closable?: boolean;
  data?: any;
  id?: any;
}
