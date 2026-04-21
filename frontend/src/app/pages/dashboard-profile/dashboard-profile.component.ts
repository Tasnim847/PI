import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

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

        if (user.id && this.isBrowser) {
          localStorage.setItem('userId', user.id.toString());
        }

        if (user.photo) {
          this.profilePhoto = `http://localhost:8083/uploads/${user.photo}`;
        }
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
        // ✅ Backend retourne une string, pas un objet {success, message}
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

  onPhotoSelected(event: any) {
    if (!this.isBrowser) return;

    const file = event.target.files[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      alert('❌ File size must be less than 5MB');
      return;
    }

    if (!file.type.startsWith('image/')) {
      alert('❌ Please select an image file');
      return;
    }

    const userId = this.auth.getUserId();
    if (!userId) {
      alert('❌ User ID not found');
      return;
    }

    this.auth.uploadPhoto(userId, file).subscribe({
      next: (response: any) => {
        console.log('Photo upload response:', response);
        alert('✅ Profile photo updated!');
        this.loadUserData();
      },
      error: (err) => {
        console.error('Photo upload error:', err);
        alert('❌ Error uploading photo');
      }
    });
  }

  logout() {
    if (this.isBrowser) {
      this.auth.logout();
    }
    this.router.navigate(['/']);
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
    formData.append('telephone', this.telephone);

    this.auth.updateProfile(userId, formData).subscribe({
      next: (response: any) => {
        console.log('Profile update response:', response);
        alert('✅ Profile updated successfully');
        this.loadUserData();
        this.isEditing = false;
      },
      error: (err) => {
        console.error('Profile update error:', err);
        if (err.status === 403) {
          alert('❌ You don\'t have permission to update this profile');
        } else if (err.error && typeof err.error === 'string') {
          alert(`❌ ${err.error}`);
        } else {
          alert('❌ Error updating profile');
        }
      }
    });
  }
}
