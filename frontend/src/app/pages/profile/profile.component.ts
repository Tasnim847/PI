import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { FaceAuthService } from '../../services/face-auth.service';  // ✅ À AJOUTER

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  firstName: string = '';
  lastName: string = '';
  email: string = '';
  telephone: string = '';
  role: string = '';
  profilePhoto: string = '';
  currentDate: Date = new Date();

  isEditing: boolean = false;

  // Password change
  currentPassword: string = '';
  newPassword: string = '';
  confirmPassword: string = '';

  // Backup for cancel
  private backupData: any = {};

  // ✅ AJOUTER CES VARIABLES POUR LA RECONNAISSANCE FACIALE
  hasFaceRegistered: boolean = false;
  faceStatusMessage: string = '';
  faceStatusLoading: boolean = false;

  constructor(
    private auth: AuthService,
    private faceAuth: FaceAuthService,  // ✅ À AJOUTER
    private router: Router
  ) {}

  ngOnInit() {
    this.checkAuth();
    this.loadUserData();
    this.checkFaceStatus();  // ✅ À AJOUTER
  }

  checkAuth() {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/public/login']);
      return;
    }
  }

  loadUserData() {
    this.auth.getMe().subscribe(user => {
      this.firstName = user.firstName;
      this.lastName = user.lastName;
      this.email = user.email;
      this.telephone = user.telephone;
      this.role = user.role;
      if (user.photo) {
        this.profilePhoto = `http://localhost:8081/uploads/${user.photo}`;
      } else {
        this.profilePhoto = '';
      }
    });
  }

  // ✅ AJOUTER CETTE MÉTHODE
  checkFaceStatus() {
    this.faceAuth.getFaceStatus().subscribe({
      next: (res) => {
        this.hasFaceRegistered = res.hasFaceRegistered;
      },
      error: () => {
        this.hasFaceRegistered = false;
      }
    });
  }

  // ✅ AJOUTER CETTE MÉTHODE
  goToFaceRegister() {
    this.router.navigate(['/face-register']);
  }

  // ✅ AJOUTER CETTE MÉTHODE
  deleteFace() {
    if (confirm('Êtes-vous sûr de vouloir supprimer votre visage enregistré ?')) {
      this.faceStatusLoading = true;
      this.faceAuth.deleteFace().subscribe({
        next: () => {
          this.hasFaceRegistered = false;
          this.faceStatusMessage = '✅ Visage supprimé avec succès';
          setTimeout(() => this.faceStatusMessage = '', 3000);
          this.faceStatusLoading = false;
        },
        error: (err) => {
          this.faceStatusMessage = '❌ Erreur lors de la suppression';
          setTimeout(() => this.faceStatusMessage = '', 3000);
          this.faceStatusLoading = false;
        }
      });
    }
  }

  getRoleName(): string {
    const roles: { [key: string]: string } = {
      'CLIENT': 'Client',
      'ADMIN': 'Administrateur',
      'AGENT_FINANCE': 'Agent Finance',
      'AGENT_ASSURANCE': 'Agent Assurance'
    };
    return roles[this.role] || this.role;
  }

  enableEditing() {
    this.isEditing = true;
    this.backupData = {
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      telephone: this.telephone
    };
  }

  cancelEditing() {
    this.isEditing = false;
    this.firstName = this.backupData.firstName;
    this.lastName = this.backupData.lastName;
    this.email = this.backupData.email;
    this.telephone = this.backupData.telephone;
  }

  changePassword() {
    if (this.newPassword !== this.confirmPassword) {
      alert('❌ Les mots de passe ne correspondent pas');
      return;
    }

    const role = this.auth.getRole();
    const userId = this.auth.getUserId();

    const request = {
      id: userId,
      oldPassword: this.currentPassword,
      newPassword: this.newPassword
    };

    this.auth.changePassword(role, request).subscribe({
      next: () => {
        alert('✅ Mot de passe modifié avec succès');
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
      },
      error: () => {
        alert('❌ erreur ou ancien mot de passe incorrect');
      }
    });
  }

  onPhotoSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.profilePhoto = e.target.result;
        localStorage.setItem('profilePhoto', this.profilePhoto);
        alert('✅ Photo de profil mise à jour !');
      };
      reader.readAsDataURL(file);
    }
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/']);
  }

  updateProfile() {
    const data = {
      firstName: this.firstName,
      lastName: this.lastName,
      telephone: this.telephone
    };

    this.auth.updateMe(data).subscribe({
      next: () => {
        alert("Profil mis à jour");
        this.loadUserData();
        this.isEditing = false;
      },
      error: (err) => {
        console.error(err);
        alert("Erreur update");
      }
    });
  }
}
