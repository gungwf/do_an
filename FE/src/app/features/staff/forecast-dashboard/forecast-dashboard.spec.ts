import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForecastDashboard } from './forecast-dashboard';

describe('ForecastDashboard', () => {
  let component: ForecastDashboard;
  let fixture: ComponentFixture<ForecastDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForecastDashboard]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForecastDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
