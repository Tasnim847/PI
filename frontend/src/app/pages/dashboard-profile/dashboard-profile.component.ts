// dashboard-profile.component.ts (Version corrigée)

import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserInfo } from '../../services/auth.service';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-dashboard-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard-profile.component.html',
  styleUrls: ['./dashboard-profile.component.css']
})
export class DashboardProfileComponent implements OnInit {
  firstName: string = '';
  lastName: string = '';
  email: string = '';
  telephone: string = '';
  role: string = '';
  profilePhoto: string = '';
  currentDate: Date = new Date();

  isEditing: boolean = false;

  currentPassword: string = '';
  newPassword: string = '';
  confirmPassword: string = '';

  private backupData: any = {};
  private isBrowser: boolean;

  constructor(
    private auth: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    if (this.isBrowser) {
      this.checkAuth();
      this.loadUserData();
    }
  }

  checkAuth() {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/public/login']);
      return;
    }
  }

  loadUserData() {
    if (!this.isBrowser) return;

    if (!this.auth.getToken()) {
      console.log('No token found, skipping user data load');
      return;
    }

    this.auth.getMe().subscribe({
      next: (user) => {
        console.log('User data loaded:', user);
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.email = user.email;
        this.telephone = user.telephone;
        this.role = user.role;

        // Mettre à jour le localStorage
        if (user.id && this.isBrowser) {
          localStorage.setItem('userId', user.id.toString());
          localStorage.setItem('firstName', user.firstName);
          localStorage.setItem('lastName', user.lastName);
          localStorage.setItem('userEmail', user.email);
          localStorage.setItem('role', user.role);
          
          // Mettre à jour l'objet user_info
          const userInfo: UserInfo = {
            id: user.id,
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
            role: user.role,
            telephone: user.telephone,
            photo: user.photo
          };
          localStorage.setItem('user_info', JSON.stringify(userInfo));
        }

        // 🔥 CORRECTION : Ajouter un timestamp pour éviter le cache
        if (user.photo) {
          this.profilePhoto = `http://localhost:8081/uploads/${user.photo}?t=${new Date().getTime()}`;
        } else {
          this.profilePhoto = '';
        }
        
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading user data:', err);
        if (err.status === 401) {
          this.router.navigate(['/public/login']);
        }
      }
    });
  }

  getRoleName(): string {
    const roles: { [key: string]: string } = {
      'CLIENT': 'Client',
      'ADMIN': 'Administrator',
      'AGENT_FINANCE': 'Finance Agent',
      'AGENT_ASSURANCE': 'Insurance Agent'
    };
    return roles[this.role] || this.role;
  }

  getRoleClass(): string {
    const roleClasses: { [key: string]: string } = {
      'CLIENT': 'role-client',
      'ADMIN': 'role-admin',
      'AGENT_FINANCE': 'role-finance',
      'AGENT_ASSURANCE': 'role-assurance'
    };
    return roleClasses[this.role] || 'role-default';
  }

  enableEditing() {
    this.isEditing = true;
    this.backupData = {
      firstName: this.firstName,
      lastName: this.lastName,
      telephone: this.telephone
    };
  }

  cancelEditing() {
    this.isEditing = false;
    this.firstName = this.backupData.firstName;
    this.lastName = this.backupData.lastName;
    this.telephone = this.backupData.telephone;
  }

  changePassword() {
    if (!this.isBrowser) return;

    if (this.newPassword !== this.confirmPassword) {
      alert('❌ Passwords do not match');
      return;
    }

    if (this.newPassword.length < 6) {
      alert('❌ Password must be at least 6 characters');
      return;
    }

    if (!this.currentPassword) {
      alert('❌ Please enter your current password');
      return;
    }

    const role = this.auth.getRole();
    const userId = this.auth.getUserId();

    if (!userId) {
      alert('❌ User ID not found. Please login again.');
      this.router.navigate(['/public/login']);
      return;
    }

    const request = {
      id: userId,
      oldPassword: this.currentPassword,
      newPassword: this.newPassword
    };

    this.auth.changePassword(role, request).subscribe({
      next: (response: any) => {
        console.log('Password change response:', response);
        alert('✅ Password changed successfully');
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
      },
      error: (err) => {
        console.error('Password change error:', err);
        if (err.error && typeof err.error === 'string') {
          alert('❌ ' + err.error);
        } else if (err.status === 400) {
          alert('❌ Current password is incorrect');
        } else if (err.status === 403) {
          alert('❌ You don\'t have permission to change this password');
        } else {
          alert('❌ Error changing password. Please try again.');
        }
      }
    });
  }

  // Dans dashboard-profile.component.ts

// Version améliorée avec prévisualisation

onPhotoSelected(event: any) {
    if (!this.isBrowser) return;

    const file = event.target.files[0];
    if (!file) return;

    // Validation
    if (file.size > 5 * 1024 * 1024) {
        alert('❌ File size must be less than 5MB');
        return;
    }

    if (!file.type.startsWith('image/')) {
        alert('❌ Please select an image file');
        return;
    }

    // Afficher l'aperçu immédiatement
    const reader = new FileReader();
    reader.onload = (e: any) => {
        this.profilePhoto = e.target.result; // Aperçu local
        this.cdr.detectChanges();
    };
    reader.readAsDataURL(file);

    const userId = this.auth.getUserId();
    if (!userId) {
        alert('❌ User ID not found');
        return;
    }

    // Upload
    const formData = new FormData();
    formData.append('firstName', this.firstName);
    formData.append('lastName', this.lastName);
    formData.append('email', this.email);
    formData.append('telephone', this.telephone);
    formData.append('photo', file);

    this.auth.updateProfileWithPhoto(userId, formData).subscribe({
        next: (response: any) => {
            console.log('Profile update response:', response);
            alert('✅ Profile photo updated!');
            
            // Récupérer l'URL complète avec timestamp anti-cache
            if (response && response.photo) {
                const timestamp = new Date().getTime();
                this.profilePhoto = `http://localhost:8081/uploads/${response.photo}?t=${timestamp}`;
                
                // Mettre à jour le localStorage
                const currentUser = this.auth.getCurrentUser();
                if (currentUser) {
                    currentUser.photo = response.photo;
                    this.auth.setCurrentUser(currentUser);
                }
            }
            
            this.cdr.detectChanges();
        },
        error: (err) => {
            console.error('Photo upload error:', err);
            alert('❌ Error uploading photo');
            // Recharger l'ancienne photo en cas d'erreur
            this.loadUserData();
        }
    });
}

updateProfile() {
    if (!this.isBrowser) return;

    const userId = this.auth.getUserId();
    if (!userId) {
        alert('❌ User ID not found');
        return;
    }

    const formData = new FormData();
    formData.append('firstName', this.firstName);
    formData.append('lastName', this.lastName);
    formData.append('email', this.email);
    formData.append('telephone', this.telephone);

    this.auth.updateProfile(userId, formData).subscribe({
        next: (response: any) => {
            console.log('Profile update response:', response);
            alert('✅ Profile updated successfully');
            
            // Mettre à jour la photo si elle a changée
            if (response.photo) {
                this.profilePhoto = `http://localhost:8081/uploads/${response.photo}?t=${new Date().getTime()}`;
            }
            
            this.loadUserData();
            this.isEditing = false;
        },
        error: (err) => {
            console.error('Profile update error:', err);
            alert('❌ Error updating profile');
        }
    });
}
  logout() {
    if (this.isBrowser) {
      this.auth.logout();
    }
    this.router.navigate(['/']);
  }

  // Dans dashboard-profile.component.ts

handleImageError(event: any) {
    console.warn('Failed to load image:', this.profilePhoto);
    event.target.style.display = 'none';
    
    // Afficher le placeholder
    const wrapper = event.target.parentElement;
    if (wrapper) {
        const placeholder = wrapper.querySelector('.profile-photo-placeholder');
        if (placeholder) {
            placeholder.classList.remove('d-none');
            placeholder.classList.add('d-flex');
        }
    }
    
    // Réinitialiser pour éviter les erreurs répétées
    if (this.profilePhoto) {
        this.profilePhoto = '';
    }
    this.cdr.detectChanges();
}
}