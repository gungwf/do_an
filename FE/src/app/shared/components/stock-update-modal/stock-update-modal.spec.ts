import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StockUpdateModal } from './stock-update-modal';

describe('StockUpdateModal', () => {
  let component: StockUpdateModal;
  let fixture: ComponentFixture<StockUpdateModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StockUpdateModal]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StockUpdateModal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
