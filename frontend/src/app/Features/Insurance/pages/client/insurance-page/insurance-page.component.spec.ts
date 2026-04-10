import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InsurancePageComponent } from './insurance-page.component';

describe('InsurancePageComponent', () => {
  let component: InsurancePageComponent;
  let fixture: ComponentFixture<InsurancePageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InsurancePageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InsurancePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
