import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConnectionMapComponent } from './connection-map.component';

describe('ConnectionMapComponent', () => {
  let component: ConnectionMapComponent;
  let fixture: ComponentFixture<ConnectionMapComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConnectionMapComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConnectionMapComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
