import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListMyCompensationsComponent } from './list-my-compensations.component';

describe('ListMyCompensationsComponent', () => {
  let component: ListMyCompensationsComponent;
  let fixture: ComponentFixture<ListMyCompensationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListMyCompensationsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListMyCompensationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
