import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AgentContractsComponent } from './agent-contracts.component';

describe('AgentContractsComponent', () => {
  let component: AgentContractsComponent;
  let fixture: ComponentFixture<AgentContractsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentContractsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentContractsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
