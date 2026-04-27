import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AgentCompensationListComponent } from './agent-compensation-list.component';

describe('AgentCompensationListComponent', () => {
  let component: AgentCompensationListComponent;
  let fixture: ComponentFixture<AgentCompensationListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentCompensationListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentCompensationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
