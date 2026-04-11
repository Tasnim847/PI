import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListAllCompensationsComponent } from './list-all-compensations.component';

describe('ListAllCompensationsComponent', () => {
  let component: ListAllCompensationsComponent;
  let fixture: ComponentFixture<ListAllCompensationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListAllCompensationsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListAllCompensationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
