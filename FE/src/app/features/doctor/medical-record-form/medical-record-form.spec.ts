import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MedicalRecordForm } from './medical-record-form';

describe('MedicalRecordForm', () => {
  let component: MedicalRecordForm;
  let fixture: ComponentFixture<MedicalRecordForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MedicalRecordForm]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MedicalRecordForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
