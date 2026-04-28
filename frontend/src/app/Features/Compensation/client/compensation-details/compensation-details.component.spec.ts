import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CompensationDetailsComponent } from './compensation-details.component';

describe('CompensationDetailsComponent', () => {
  let component: CompensationDetailsComponent;
  let fixture: ComponentFixture<CompensationDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompensationDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CompensationDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
