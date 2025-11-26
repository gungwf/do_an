import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StaffLayout } from './staff-layout';

describe('StaffLayout', () => {
  let component: StaffLayout;
  let fixture: ComponentFixture<StaffLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StaffLayout]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StaffLayout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
