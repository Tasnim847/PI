import { Component, EventEmitter, Output } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {
  @Output() sidebarToggle = new EventEmitter<boolean>();

  menuItems = [
    { label: 'Home', icon: '🏠', link: '/dashboard' },
    { label: 'Credit', icon: '💳', link: '/dashboard/credit' },
    { label: 'Insurance', icon: '🛡️', link: '/backoffice/insurance' },
    { label: 'Account', icon: '👤', link: '/dashboard/account' },
    { label: 'Complaint', icon: '📄', link: '/dashboard/complaint' },
    { label: 'News', icon: '📰', link: '/dashboard/news' },
    { label: 'Products', icon: '📦', link: '/backoffice/products' }
  ];

  isCollapsed = false;

  toggleSidebar() {
    this.isCollapsed = !this.isCollapsed;
    
    if (this.isCollapsed) {
      document.body.classList.add('sidebar-collapsed');
    } else {
      document.body.classList.remove('sidebar-collapsed');
    }
    
    // Émettre l'événement
    this.sidebarToggle.emit(this.isCollapsed);
  }
}