import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InssAgentDashboardComponent } from './inss-agent-dashboard.component';

describe('InssAgentDashboardComponent', () => {
  let component: InssAgentDashboardComponent;
  let fixture: ComponentFixture<InssAgentDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InssAgentDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InssAgentDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
