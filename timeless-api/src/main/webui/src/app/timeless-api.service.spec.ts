import { TestBed } from '@angular/core/testing';

import { TimelessApiService } from './timeless-api.service';

describe('TimelessApiService', () => {
  let service: TimelessApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TimelessApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
