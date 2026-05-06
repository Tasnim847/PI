import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { FaceAuthService } from '../../services/face-auth.service';

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
  profilePhoto: string = 'assets/default-avatar.png';
  currentDate: Date = new Date();

  isEditing: boolean = false;

  currentPassword: string = '';
  newPassword: string = '';
  confirmPassword: string = '';

  private backupData: any = {};

  hasFaceRegistered: boolean = false;
  faceStatusMessage: string = '';
  faceStatusLoading: boolean = false;

  private isBrowser: boolean;

  constructor(
    private auth: AuthService,
    private faceAuth: FaceAuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    // ✅ Guard SSR global
    if (!this.isBrowser) return;

    this.checkAuth();
    this.loadUserData();
    this.checkFaceStatus();
  }

  checkAuth() {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/public/login']);
    }
  }

  loadUserData() {
  if (!this.isBrowser) return;

  this.auth.getMe().subscribe({
    next: (user) => {
      console.log('User reçu:', user);
      console.log('Photo:', user.photo);

      this.firstName = user.firstName;
      this.lastName = user.lastName;
      this.email = user.email;
      this.telephone = user.telephone;
      this.role = user.role;

      if (user.photo && user.photo !== 'null' && user.photo !== '') {
        if (user.photo.startsWith('http')) {
          this.profilePhoto = user.photo;
        } else if (user.photo.startsWith('uploads/')) {
          this.profilePhoto = `http://localhost:8083/${user.photo}`;
        } else {
          this.profilePhoto = `http://localhost:8083/uploads/${user.photo}`;
        }
      } else {
        this.profilePhoto = 'assets/default-avatar.png';
      }

      console.log('URL photo finale:', this.profilePhoto);
    },
    error: (err) => {
      console.error('Erreur getMe:', err);
      this.profilePhoto = 'assets/default-avatar.png';
    }
  });
}
  checkFaceStatus() {
    if (!this.isBrowser) return;

    this.faceAuth.getFaceStatus().subscribe({
      next: (res) => {
        this.hasFaceRegistered = res.hasFaceRegistered;
      },
      error: () => {
        this.hasFaceRegistered = false;
      }
    });
  }

  goToFaceRegister() {
    this.router.navigate(['/face-register']);
  }

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
          console.error('Status:', err.status);
          console.error('Error:', err.error);
          this.faceStatusMessage = `❌ Erreur ${err.status}: ${err.error || 'Suppression échouée'}`;
          setTimeout(() => this.faceStatusMessage = '', 5000);
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

    this.auth.changePassword(role, {
      id: userId,
      oldPassword: this.currentPassword,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        alert('✅ Mot de passe modifié avec succès');
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
      },
      error: () => {
        alert('❌ Erreur ou ancien mot de passe incorrect');
      }
    });
  }

  onPhotoSelected(event: any) {
    if (!this.isBrowser) return;

    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.profilePhoto = e.target.result;
      alert('✅ Photo de profil mise à jour !');
    };
    reader.readAsDataURL(file);
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
        alert('Profil mis à jour');
        this.loadUserData();
        this.isEditing = false;
      },
      error: (err) => {
        console.error(err);
        alert('Erreur update');
      }
    });
  }
  getInitials(): string {
  const f = this.firstName ? this.firstName.charAt(0).toUpperCase() : '';
  const l = this.lastName ? this.lastName.charAt(0).toUpperCase() : '';
  return f + l;
}
}