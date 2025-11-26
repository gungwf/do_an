import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CheckoutDialog } from './checkout-dialog';

describe('CheckoutDialog', () => {
  let component: CheckoutDialog;
  let fixture: ComponentFixture<CheckoutDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CheckoutDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CheckoutDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
