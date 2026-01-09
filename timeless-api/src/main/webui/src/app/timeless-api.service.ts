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
    return this.httpClient.get<RecordPageResponse>('/api/records', {
      params,
    });
  }

  signUp(value: any) {
    return this.httpClient.post('/api/sign-up', {
      ...value,
    });
  }

  updateUser(updateUser: UpdateUser) {
    const data = localStorage.getItem(timelessLocalStorageKey);
    if (data == null) {
      throw new Error();
    }

    const user = JSON.parse(data);
    return this.httpClient.put('/api/users', {
      id: user.id,
      firstName: updateUser.firstName,
      lastName: updateUser.lastName,
      email: updateUser.email,
      phoneNumber: (updateUser.phoneNumber || '').replace(notNumbers, ''),
    });
  }

  userInfo() {
    const data = localStorage.getItem(timelessLocalStorageKey);
    if (data == null) {
      throw new Error();
    }
    const user = JSON.parse(data);
    return this.httpClient.get(`/api/users/${user.id}`);
  }

  deleteRecord(id: number) {
    return this.httpClient.delete(`/api/records/${id}`);
  }

  updateRecord(id: number, record: UpdateRecord) {
    return this.httpClient.put(`/api/records/${id}`, record);
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
  id: number;
  amount: number;
  description: string;
  transaction: 'IN' | 'OUT';
  date: string;
  createdAt: string;
  category: string;
  tag?: string;
  icon?: string;
}

export interface UpdateUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
}

export interface UpdateRecord {
  id: number;
  amount: number;
  description: string;
  transaction: 'IN' | 'OUT';
  date: string;
  category: string;
  tag?: string;
}
