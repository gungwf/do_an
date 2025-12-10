import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MedicalRecordDetailDialog } from './medical-record-detail-dialog';

describe('MedicalRecordDetailDialog', () => {
  let component: MedicalRecordDetailDialog;
  let fixture: ComponentFixture<MedicalRecordDetailDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MedicalRecordDetailDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MedicalRecordDetailDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
