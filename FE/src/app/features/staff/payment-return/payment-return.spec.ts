import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaymentReturn } from './payment-return';

describe('PaymentReturn', () => {
  let component: PaymentReturn;
  let fixture: ComponentFixture<PaymentReturn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentReturn]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PaymentReturn);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
