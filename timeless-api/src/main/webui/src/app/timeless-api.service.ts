import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TimelessApiService {

  constructor(private readonly httpClient: HttpClient) { }

  signIn(email: string, password: string): Observable<SignInResponse> {
    return this.httpClient.post<SignInResponse>("api/sign-in", {
      email,
      password
    })
  }
}

interface SignInResponse {
  token: string
}
