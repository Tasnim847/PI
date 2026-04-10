import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContractRiskDetailsComponent } from './contract-risk-details.component';

describe('ContractRiskDetailsComponent', () => {
  let component: ContractRiskDetailsComponent;
  let fixture: ComponentFixture<ContractRiskDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContractRiskDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ContractRiskDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
