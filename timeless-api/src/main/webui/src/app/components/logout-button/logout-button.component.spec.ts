import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LogoutButtonComponent } from './logout-button.component';
import { TimelessApiService } from '../../timeless-api.service';
import { Router } from '@angular/router';

describe('LogoutButtonComponent', () => {
  let component: LogoutButtonComponent;
  let fixture: ComponentFixture<LogoutButtonComponent>;
  let mockTimelessApiService: jasmine.SpyObj<TimelessApiService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    mockTimelessApiService = jasmine.createSpyObj('TimelessApiService', [
      'logout',
    ]);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [LogoutButtonComponent],
      providers: [
        { provide: TimelessApiService, useValue: mockTimelessApiService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LogoutButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call logout and navigate to root on click', () => {
    component.onLogout();
    expect(mockTimelessApiService.logout).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should display default text', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.text')?.textContent).toContain('Sair');
  });

  it('should display custom text', () => {
    component.textButton = 'Custom Logout';
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.text')?.textContent).toContain(
      'Custom Logout',
    );
  });

  it('should not logout when disabled', () => {
    component.disabled = true;
    fixture.detectChanges();
    component.onLogout();
    expect(mockTimelessApiService.logout).not.toHaveBeenCalled();
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });
});
