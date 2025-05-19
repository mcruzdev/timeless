import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TimelessApiService {

  constructor(private readonly httpClient: HttpClient) {
  }

  signIn(email: string, password: string): Observable<SignInResponse> {
    return this.httpClient.post<SignInResponse>("api/sign-in", {
      email,
      password
    })
  }

  getRecords(): Observable<RecordResponseItem[]> {
    return this.httpClient.get<RecordResponseItem[]>('/api/records', {
      headers: {
        Authorization: `Bearer ${localStorage.getItem("timeless-api::token")}`
      },
    })
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
