import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StaffProfile } from './staff-profile';

describe('StaffProfile', () => {
  let component: StaffProfile;
  let fixture: ComponentFixture<StaffProfile>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StaffProfile]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StaffProfile);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
