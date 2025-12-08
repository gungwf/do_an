import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrescriptionDialog } from './prescription-dialog';

describe('PrescriptionDialog', () => {
  let component: PrescriptionDialog;
  let fixture: ComponentFixture<PrescriptionDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrescriptionDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PrescriptionDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
