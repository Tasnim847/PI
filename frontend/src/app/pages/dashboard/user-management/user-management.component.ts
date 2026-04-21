import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AdminService, User } from '../../../services/admin.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit {
  type: 'clients' | 'agents-assurance' | 'agents-finance' | 'admins' = 'clients';

  users: User[] = [];
  filteredUsers: User[] = [];
  searchTerm = '';
  showModal = false;
  isEditMode = false;
  selectedUserId: number | null = null;
  formData: any = {};
  selectedFile: File | null = null;
  loading = false;
  error: string | null = null;

  constructor(
    private adminService: AdminService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.route.data.subscribe(data => {
      this.type = data['type'] || 'clients';
      this.users = [];
      this.filteredUsers = [];
      this.loadUsers();
    });

    // ✅ Ouvrir le modal si queryParam openAdd=true
    this.route.queryParams.subscribe(params => {
      if (params['openAdd'] === 'true') {
        this.openAddModal();
      }
    });
  }

  getTitle(): string {
    const titles: any = {
      'clients': 'Client Management',
      'agents-assurance': 'Insurance Agent Management',
      'agents-finance': 'Finance Agent Management',
      'admins': 'Administrator Management'
    };
    return titles[this.type];
  }

  getTypeLabel(): string {
    const labels: any = {
      'clients': 'Client',
      'agents-assurance': 'Insurance Agent',
      'agents-finance': 'Finance Agent',
      'admins': 'Administrator'
    };
    return labels[this.type];
  }

  loadUsers() {
    this.loading = true;
    this.error = null;

    const methods: any = {
      'clients':          () => this.adminService.getClients(),
      'agents-assurance': () => this.adminService.getAgentsAssurance(),
      'agents-finance':   () => this.adminService.getAgentsFinance(),
      'admins':           () => this.adminService.getAdmins()
    };

    methods[this.type]().subscribe({
      next: (data: User[]) => {
        this.users = data;
        this.filteredUsers = [...data];
        this.loading = false;
      },
      error: (err: any) => {
        console.error(`❌ Erreur chargement ${this.type}:`, err);
        this.error = err.status === 403
          ? 'Access denied. Please log in again.'
          : err.message || 'Loading error';
        this.loading = false;
      }
    });
  }

  filterUsers() {
    this.filteredUsers = this.users.filter(user =>
      `${user.firstName} ${user.lastName}`.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
      user.telephone.includes(this.searchTerm)
    );
  }

  openAddModal() {
    this.isEditMode = false;
    this.formData = {};
    this.selectedFile = null;
    this.showModal = true;
  }

  openEditModal(user: User) {
    this.isEditMode = true;
    this.selectedUserId = user.id;
    this.formData = { ...user };
    delete this.formData.password;
    this.selectedFile = null;
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.formData = {};
    this.selectedFile = null;
  }

  closeModalOnBackdrop(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.closeModal();
    }
  }

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
  }

  saveUser() {
    const formData = new FormData();
    Object.keys(this.formData).forEach(key => {
      if (this.formData[key] && key !== 'photo') {
        formData.append(key, this.formData[key]);
      }
    });
    if (this.selectedFile) {
      formData.append('photo', this.selectedFile);
    }

    this.loading = true;

    const methods: any = {
      'clients':          () => this.isEditMode ? this.adminService.updateClient(this.selectedUserId!, formData)        : this.adminService.addClient(formData),
      'agents-assurance': () => this.isEditMode ? this.adminService.updateAgentAssurance(this.selectedUserId!, formData) : this.adminService.addAgentAssurance(formData),
      'agents-finance':   () => this.isEditMode ? this.adminService.updateAgentFinance(this.selectedUserId!, formData)   : this.adminService.addAgentFinance(formData),
      'admins':           () => this.isEditMode ? this.adminService.updateAdmin(this.selectedUserId!, formData)          : this.adminService.addAdmin(formData)
    };

    methods[this.type]().subscribe({
      next: () => {
        this.loadUsers();
        this.closeModal();
        this.loading = false;
      },
      error: (err: any) => {
        console.error('❌ Error saving user', err);
        this.loading = false;
        alert(`Error: ${err.message || 'Save failed'}`);
      }
    });
  }

  deleteUser(id: number) {
    if (confirm('Are you sure you want to delete this user?')) {
      this.loading = true;
      const methods: any = {
        'clients':          () => this.adminService.deleteClient(id),
        'agents-assurance': () => this.adminService.deleteAgentAssurance(id),
        'agents-finance':   () => this.adminService.deleteAgentFinance(id),
        'admins':           () => this.adminService.deleteAdmin(id)
      };

      methods[this.type]().subscribe({
        next: () => {
          this.loadUsers();
          this.loading = false;
        },
        error: (err: any) => {
          console.error('❌ Error deleting user', err);
          this.loading = false;
          alert(`Error: ${err.message || 'Delete failed'}`);
        }
      });
    }
  }

  retry() {
    this.loadUsers();
  }
}