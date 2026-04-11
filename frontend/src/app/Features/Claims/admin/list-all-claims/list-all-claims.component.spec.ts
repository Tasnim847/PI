import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListAllClaimsComponent } from './list-all-claims.component';

describe('ListAllClaimsComponent', () => {
  let component: ListAllClaimsComponent;
  let fixture: ComponentFixture<ListAllClaimsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListAllClaimsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListAllClaimsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
