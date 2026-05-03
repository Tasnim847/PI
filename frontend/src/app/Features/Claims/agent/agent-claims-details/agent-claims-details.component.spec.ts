import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AgentClaimsDetailsComponent } from './agent-claims-details.component';

describe('AgentClaimsDetailsComponent', () => {
  let component: AgentClaimsDetailsComponent;
  let fixture: ComponentFixture<AgentClaimsDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentClaimsDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentClaimsDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
