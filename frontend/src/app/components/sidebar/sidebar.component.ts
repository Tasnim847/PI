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

  // sidebar.component.ts
  menuItems = [
    { label: 'Home', icon: '🏠', link: '/backoffice' },  // Changé de '/dashboard' à '/backoffice'
    { label: 'Clients', icon: '👥', link: '/backoffice/clients' },
    { label: 'Agents Assurance', icon: '🛡️', link: '/backoffice/agents-assurance' },
    { label: 'Agents Finance', icon: '💰', link: '/backoffice/agents-finance' },
    { label: 'Administrateurs', icon: '👨‍💼', link: '/backoffice/admins' },
    { label: 'Credit', icon: '💳', link: '/backoffice/credit' },
    { label: 'Repayment', icon: '💵', link: '/backoffice/repayment' },
    { label: 'Insurance', icon: '🛡️', link: '/backoffice/insurance' },
    { label: 'Account', icon: '👤', link: '/backoffice/account' },
    { label: 'Complaint', icon: '📄', link: '/backoffice/complaint' },
    { label: 'KPIs', icon: '📊', link: '/backoffice/complaint/kpi' },  // ⭐ AJOUT KPI ⭐
    { label: 'News', icon: '📰', link: '/backoffice/news' },
    { label: 'Products', icon: '📦', link: '/backoffice/products' },
    { label: 'Claims', icon: '📋', link: '/backoffice/claims' },
    { label: 'Compensation', icon: '💰', link: '/backoffice/compensation' }
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
