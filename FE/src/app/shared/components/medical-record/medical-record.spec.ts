import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MedicalRecordViewDialogComponent } from './medical-record';

describe('MedicalRecord', () => {
  let component: MedicalRecordViewDialogComponent;
  let fixture: ComponentFixture<MedicalRecordViewDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MedicalRecordViewDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MedicalRecordViewDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
