import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AdminService, User, ClientWithAgents } from '../../../services/admin.service';

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
  clients: ClientWithAgents[] = [];  // 🆕 Pour stocker les clients avec leurs agents
  filteredUsers: User[] = [];
  filteredClients: ClientWithAgents[] = [];  // 🆕
  searchTerm = '';
  showModal = false;
  showAssignModal = false;
  isEditMode = false;
  selectedUserId: number | null = null;
  selectedUser: ClientWithAgents | null = null;  // 🆕 Type ClientWithAgents
  formData: any = {};
  selectedFile: File | null = null;
  loading = false;
  error: string | null = null;

  agentsFinance: User[] = [];
  agentsAssurance: User[] = [];
  selectedAgentType: 'FINANCE' | 'ASSURANCE' = 'FINANCE';
  selectedAgentId: number | null = null;
  assignLoading = false;

  constructor(
    private adminService: AdminService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.route.data.subscribe(data => {
      this.type = data['type'] || 'clients';
      this.loadUsers();
      
      if (this.type === 'clients') {
        this.loadAgents();
      }
    });

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

    if (this.type === 'clients') {
      // Clients avec leurs agents
      this.adminService.getClients().subscribe({
        next: (data: ClientWithAgents[]) => {
          this.clients = data;
          this.filteredClients = [...data];
          this.loading = false;
        },
        error: (err: any) => {
          console.error(`❌ Erreur chargement clients:`, err);
          this.error = err.status === 403
            ? 'Access denied. Please log in again.'
            : err.message || 'Loading error';
          this.loading = false;
        }
      });
    } else {
      const methods: any = {
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
  }

  loadAgents() {
    this.adminService.getAgentsFinance().subscribe({
      next: (data) => {
        this.agentsFinance = data;
      },
      error: (err) => console.error('Erreur chargement agents finance', err)
    });

    this.adminService.getAgentsAssurance().subscribe({
      next: (data) => {
        this.agentsAssurance = data;
      },
      error: (err) => console.error('Erreur chargement agents assurance', err)
    });
  }

  filterUsers() {
    if (this.type === 'clients') {
      this.filteredClients = this.clients.filter(client =>
        `${client.firstName} ${client.lastName}`.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        client.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        client.telephone.includes(this.searchTerm)
      );
    } else {
      this.filteredUsers = this.users.filter(user =>
        `${user.firstName} ${user.lastName}`.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        user.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        user.telephone.includes(this.searchTerm)
      );
    }
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

  openAssignModal(client: ClientWithAgents) {
    this.selectedUser = client;
    this.selectedAgentType = 'FINANCE';
    this.selectedAgentId = null;
    this.showAssignModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.formData = {};
    this.selectedFile = null;
  }

  closeAssignModal() {
    this.showAssignModal = false;
    this.selectedUser = null;
    this.selectedAgentId = null;
  }

  closeModalOnBackdrop(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.closeModal();
    }
  }

  closeAssignModalOnBackdrop(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.closeAssignModal();
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
        if (this.type === 'clients') {
          this.loadAgents();
        }
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

  // 🆕 Soumettre l'affectation
  submitAssignment() {
    if (!this.selectedAgentId) {
      alert('Please select an agent');
      return;
    }

    this.assignLoading = true;

    let request;
    if (this.selectedAgentType === 'FINANCE') {
      request = this.adminService.assignFinanceAgent(this.selectedUser!.id, this.selectedAgentId);
    } else {
      request = this.adminService.assignAssuranceAgent(this.selectedUser!.id, this.selectedAgentId);
    }

    request.subscribe({
      next: () => {
        alert(`✅ Client assigned to ${this.selectedAgentType === 'FINANCE' ? 'Finance' : 'Insurance'} agent successfully!`);
        this.closeAssignModal();
        this.loadUsers(); // Recharger pour afficher les changements
        this.assignLoading = false;
      },
      error: (err) => {
        console.error('Error assigning agent:', err);
        alert(`❌ Error: ${err.error?.message || err.message}`);
        this.assignLoading = false;
      }
    });
  }

  deleteUser(id: number) {
    if (confirm('Are you sure you want to disable this user?')) {
      this.loading = true;
      
      const methods: any = {
        'clients': () => this.adminService.deleteClient(id),
        'agents-assurance': () => this.adminService.deleteAgentAssurance(id),
        'agents-finance': () => this.adminService.deleteAgentFinance(id),
        'admins': () => this.adminService.deleteAdmin(id)
      };
      
      methods[this.type]().subscribe({
        next: () => {
          this.loadUsers();
          this.loading = false;
          alert('User disabled successfully');
        },
        error: (err: any) => {
          console.error('Error disabling user', err);
          alert(`Error: ${err.message || 'Operation failed'}`);
          this.loading = false;
        }
      });
    }
  }

  // 🆕 Méthodes pour obtenir le nom des agents
  getAgentFinanceName(client: ClientWithAgents): string {
  return client.agentFinanceName || 'Not assigned';
}

getAgentAssuranceName(client: ClientWithAgents): string {
  return client.agentAssuranceName || 'Not assigned';
}
retry() {
  this.error = null;
  this.loading = true;

  this.loadUsers(); // ou loadClients() selon ton code
}
}