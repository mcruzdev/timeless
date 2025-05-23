import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegisteredComponent } from './registered.component';

describe('RegisteredComponent', () => {
  let component: RegisteredComponent;
  let fixture: ComponentFixture<RegisteredComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisteredComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisteredComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
