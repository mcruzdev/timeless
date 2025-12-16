import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { notNumbers, timelessLocalStorageKey } from './constants';

@Injectable({
  providedIn: 'root',
})
export class TimelessApiService {
  constructor(private readonly httpClient: HttpClient) {}

  signIn(value: any): Observable<SignInResponse> {
    return this.httpClient.post<SignInResponse>('api/sign-in', {
      ...value,
    });
  }

  getRecords(page: number, size: number): Observable<RecordPageResponse> {
    const httpParams = new HttpParams();
    const params = httpParams.append('page', page).append('limit', size);
    console.log(params);
    return this.httpClient.get<RecordPageResponse>('/api/records', {
      params,
    });
  }

  signUp(value: any) {
    return this.httpClient.post('/api/sign-up', {
      ...value,
    });
  }

  updatePhoneNumber(phoneNumber: string) {
    const data = localStorage.getItem(timelessLocalStorageKey);
    if (data == null) {
      throw new Error();
    }

    const user = JSON.parse(data);
    return this.httpClient.patch('/api/users', {
      id: user.id,
      phoneNumber: phoneNumber.replace(notNumbers, ''),
    });
  }

  userInfo() {
    const data = localStorage.getItem(timelessLocalStorageKey);
    if (data == null) {
      throw new Error();
    }
    const user = JSON.parse(data);
    return this.httpClient.get(`/api/users/${user.id}`, {
      headers: {
        Authorization: `Bearer ${user.token}`,
      },
    });
  }

  deleteRecord(id: number) {
    return this.httpClient.delete(`/api/records/${id}`);
  }

  logout() {
    localStorage.removeItem(timelessLocalStorageKey);
  }
}

interface SignInResponse {
  token: string;
}

export interface RecordPageResponse {
  items: RecordResponseItem[];
  totalRecords: number;
  totalIn: number;
  totalExpenses: number;
}

export interface RecordResponseItem {
  amount: number;
  description: string;
  transaction: string;
  createdAt: string;
}
