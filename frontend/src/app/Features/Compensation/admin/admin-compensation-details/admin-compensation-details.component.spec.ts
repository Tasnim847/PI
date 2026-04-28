import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminCompensationDetailsComponent } from './admin-compensation-details.component';

describe('AdminCompensationDetailsComponent', () => {
  let component: AdminCompensationDetailsComponent;
  let fixture: ComponentFixture<AdminCompensationDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminCompensationDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminCompensationDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
