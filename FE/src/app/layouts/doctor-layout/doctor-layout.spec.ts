import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DoctorLayout } from './doctor-layout';

describe('DoctorLayout', () => {
  let component: DoctorLayout;
  let fixture: ComponentFixture<DoctorLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DoctorLayout]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DoctorLayout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
