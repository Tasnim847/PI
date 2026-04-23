// src/app/Features/Complaint/pages/complaint-page/complaint-page.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { ComplaintService } from '../../../../services/complaint.service';
import { AuthService } from '../../../../services/auth.service';
import { AdminService } from '../../../../services/admin.service'; // ⭐ Utiliser AdminService existant

@Component({
  selector: 'app-complaint-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './complaint-page.component.html',
  styleUrl: './complaint-page.component.css'
})
export class ComplaintPageComponent implements OnInit {
  
  complaints: any[] = [];
  selectedComplaint: any = null;
  complaintForm: FormGroup;
  isEditing = false;
  showForm = false;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';
  selectedStatus = '';
  
  // ⭐ Pour l'admin - liste des clients (via AdminService)
  clients: any[] = [];
  
  currentClientId: number | null = null;
  currentClientEmail: string | null = null;
  currentUser: any = null;
  currentUserRole: string | null = null;

  constructor(
    private complaintService: ComplaintService,
    private authService: AuthService,
    private adminService: AdminService, // ⭐ Injection de AdminService
    private router: Router,
    private fb: FormBuilder
  ) {
    this.complaintForm = this.createForm();
  }

  ngOnInit(): void {
    this.checkAuthentication();
  }

  goToKpi(): void {
    console.log('📊 Navigation vers le tableau de bord KPI');
    this.router.navigate(['/backoffice/complaint/kpi']);
  }

  checkAuthentication(): void {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');
    
    console.log('Token présent:', !!token);
    console.log('Rôle:', role);
    this.currentUserRole = role;
    
    if (!token) {
      this.errorMessage = 'Veuillez vous connecter';
      setTimeout(() => this.router.navigate(['/login']), 2000);
      return;
    }
    
    this.loadCurrentUser();
  }
  
  loadCurrentUser(): void {
    this.isLoading = true;
    this.authService.getMe().subscribe({
      next: (user: any) => {
        console.log('Utilisateur récupéré:', user);
        this.currentUser = user;
        this.currentClientId = user.id;
        this.currentClientEmail = user.email;
        
        // ⭐ Si admin, charger la liste des clients via AdminService
        if (this.currentUserRole === 'ADMIN') {
          this.loadClients();
        }
        
        this.loadComplaints();
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Erreur /me:', err);
        this.errorMessage = 'Erreur de chargement';
        this.isLoading = false;
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  // ⭐ Charger la liste des clients via AdminService
  loadClients(): void {
    this.adminService.getClients().subscribe({
      next: (clients: any[]) => {
        this.clients = clients;
        console.log('📋 Clients chargés:', this.clients.length);
      },
      error: (err: any) => {
        console.error('Erreur chargement clients:', err);
      }
    });
  }

  private createForm(): FormGroup {
    return this.fb.group({
      message: ['', [Validators.required, Validators.minLength(5)]],
      phone: ['', Validators.required],
      status: ['PENDING'],
      clientId: [null] // ⭐ Pour l'admin
    });
  }

  loadComplaints(): void {
    this.isLoading = true;
    console.log('🔍 Chargement des réclamations...');
    
    this.complaintService.getComplaints().subscribe({
      next: (complaintsList: any[]) => {
        console.log('📦 Réclamations reçues:', complaintsList);
        this.complaints = complaintsList;
        console.log(`✅ ${this.complaints.length} réclamation(s) chargée(s)`);
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('❌ Erreur chargement:', err);
        this.errorMessage = 'Erreur lors du chargement';
        this.isLoading = false;
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  get filteredComplaints(): any[] {
    let filtered = this.complaints;
    
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(c => 
        c.message?.toLowerCase().includes(term) ||
        c.client?.firstName?.toLowerCase().includes(term) ||
        c.client?.lastName?.toLowerCase().includes(term) ||
        c.client?.email?.toLowerCase().includes(term)
      );
    }
    
    if (this.selectedStatus) {
      filtered = filtered.filter(c => c.status === this.selectedStatus);
    }
    
    return filtered;
  }

  openAddForm(): void {
    this.isEditing = false;
    this.showForm = true;
    this.selectedComplaint = null;
    this.complaintForm.reset({
      message: '',
      phone: '',
      status: 'PENDING',
      clientId: null
    });
  }

  editComplaint(complaint: any, event: Event): void {
    event.stopPropagation();
    this.isEditing = true;
    this.showForm = true;
    this.selectedComplaint = complaint;
    this.complaintForm.patchValue({
      message: complaint.message,
      phone: complaint.phone || '',
      status: complaint.status || 'PENDING',
      clientId: complaint.client?.id || null
    });
  }

  viewDetails(complaint: any): void {
    this.selectedComplaint = complaint;
  }

  closeDetails(): void {
    this.selectedComplaint = null;
  }

  saveComplaint(): void {
    if (this.complaintForm.invalid) {
      Object.keys(this.complaintForm.controls).forEach(key => {
        const control = this.complaintForm.get(key);
        if (control) control.markAsTouched();
      });
      return;
    }

    this.isLoading = true;
    const formValue = this.complaintForm.value;

    let payload: any = {
      message: formValue.message,
      phone: formValue.phone,
      status: formValue.status
    };

    // ⭐ Si admin, utiliser l'ID du client sélectionné
    if (this.currentUserRole === 'ADMIN' && formValue.clientId) {
      payload.client = { id: formValue.clientId };
    } else {
      // Si client, utiliser son email
      payload.client = { email: this.currentClientEmail };
    }

    console.log('📤 Envoi payload:', payload);

    this.complaintService.createComplaint(payload).subscribe({
      next: (response: any) => {
        console.log('✅ Réponse création:', response);
        this.successMessage = 'Réclamation créée avec succès !';
        this.afterSave();
      },
      error: (err: any) => {
        console.error('❌ Erreur création:', err);
        this.handleError(err.error?.message || 'Erreur lors de la création');
      }
    });
  }

  updateComplaint(): void {
    if (this.complaintForm.invalid || !this.selectedComplaint) {
      return;
    }

    this.isLoading = true;
    const formValue = this.complaintForm.value;

    const payload = {
      message: formValue.message,
      phone: formValue.phone,
      status: formValue.status
    };

    console.log('📤 Mise à jour du statut vers:', formValue.status);

    this.complaintService.updateComplaint(this.selectedComplaint.id, payload).subscribe({
      next: (response: any) => {
        console.log('✅ Statut mis à jour:', response);
        this.successMessage = `Statut changé à ${this.getStatusLabel(formValue.status)} !`;
        this.afterSave();
      },
      error: (err: any) => {
        console.error('❌ Erreur mise à jour:', err);
        this.handleError('Erreur lors de la modification du statut');
      }
    });
  }

  deleteComplaint(id: number, event: Event): void {
    event.stopPropagation();
    if (confirm('Supprimer cette réclamation ?')) {
      this.isLoading = true;
      this.complaintService.deleteComplaint(id).subscribe({
        next: () => {
          this.successMessage = 'Réclamation supprimée !';
          this.loadComplaints();
          this.isLoading = false;
          if (this.selectedComplaint?.id === id) this.closeDetails();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err: any) => {
          console.error(err);
          this.handleError('Erreur lors de la suppression');
        }
      });
    }
  }

  afterSave(): void {
    setTimeout(() => {
      this.loadComplaints();
      this.closeForm();
      this.isLoading = false;
    }, 500);
    setTimeout(() => this.successMessage = '', 3000);
  }

  handleError(msg: string): void {
    this.errorMessage = msg;
    this.isLoading = false;
    setTimeout(() => this.errorMessage = '', 3000);
  }

  closeForm(): void {
    this.showForm = false;
    this.isEditing = false;
    this.selectedComplaint = null;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedStatus = '';
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'PENDING': 'En attente',
      'IN_PROGRESS': 'En cours',
      'APPROVED': 'Approuvé',
      'REJECTED': 'Rejeté',
      'CLOSED': 'Fermé'
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      'PENDING': 'status-pending',
      'IN_PROGRESS': 'status-progress',
      'APPROVED': 'status-approved',
      'REJECTED': 'status-rejected',
      'CLOSED': 'status-closed'
    };
    return classes[status] || '';
  }

  getStatusIcon(status: string): string {
    const icons: Record<string, string> = {
      'PENDING': '🟡',
      'IN_PROGRESS': '🔵',
      'APPROVED': '🟢',
      'REJECTED': '🔴',
      'CLOSED': '⚫'
    };
    return icons[status] || '📋';
  }

  formatDate(date: any): string {
    if (!date) return 'Non définie';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getClientName(complaint: any): string {
    if (complaint.client?.firstName) {
      return `${complaint.client.firstName} ${complaint.client.lastName || ''}`;
    }
    return 'Client inconnu';
  }

  getClientEmail(complaint: any): string {
    return complaint.client?.email || 'Email non disponible';
  }
}