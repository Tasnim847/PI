import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListMyClaimsComponent } from './list-my-claims.component';

describe('ListMyClaimsComponent', () => {
  let component: ListMyClaimsComponent;
  let fixture: ComponentFixture<ListMyClaimsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListMyClaimsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListMyClaimsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
