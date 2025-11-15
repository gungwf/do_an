import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Appoitments } from './appoitments';

describe('Appoitments', () => {
  let component: Appoitments;
  let fixture: ComponentFixture<Appoitments>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Appoitments]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Appoitments);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
