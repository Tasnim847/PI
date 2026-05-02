import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CompAgentDashboardComponent } from './comp-agent-dashboard.component';

describe('CompAgentDashboardComponent', () => {
  let component: CompAgentDashboardComponent;
  let fixture: ComponentFixture<CompAgentDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompAgentDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CompAgentDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
