import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {notNumbers, timelessLocalStorageKey} from './constants';

@Injectable({
  providedIn: 'root'
})
export class TimelessApiService {

  constructor(private readonly httpClient: HttpClient) {
  }

  signIn(value: any): Observable<SignInResponse> {
    return this.httpClient.post<SignInResponse>("api/sign-in", {
      ...value
    })
  }

  getRecords(): Observable<RecordResponseItem[]> {
    return this.httpClient.get<RecordResponseItem[]>('/api/records')
  }

  signUp(value: any) {
    return this.httpClient.post('/api/sign-up', {
      ...value
    })
  }

  updatePhoneNumber(phoneNumber: string) {
    const data = localStorage.getItem(timelessLocalStorageKey)
    if (data == null) {
      throw new Error()
    }

    const user = JSON.parse(data)
    return this.httpClient.patch('/api/users', {
      id: user.id,
      phoneNumber: phoneNumber.replace(notNumbers, '')
    })
  }

  userInfo() {
    const data = localStorage.getItem(timelessLocalStorageKey)
    if (data == null) {
      throw new Error()
    }
    const user = JSON.parse(data)
    return this.httpClient.get(`/api/users/${user.id}`)
  }
}

interface SignInResponse {
  token: string
}

export interface RecordResponseItem {
  amount: number
  description: string
  recordType: string
  createdAt: string
}
