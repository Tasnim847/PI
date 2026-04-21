import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {

  firstName = '';
  lastName = '';
  email = '';
  password = '';
  telephone = '';
  imagePreview: string | ArrayBuffer | null = null;
  selectedFile: File | null = null;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(private auth: AuthService, private router: Router) {}

  onFileChange(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreview = reader.result;
      };
      reader.readAsDataURL(file);
    } else {
      this.imagePreview = null;
      this.selectedFile = null;
    }
  }

  onRegister() {
    // ✅ Validation champs obligatoires
    if (!this.firstName || !this.lastName || !this.email || !this.password) {
      this.errorMessage = 'Please fill in all required fields';
      return;
    }

    // ✅ Validation email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.email)) {
      this.errorMessage = 'Please enter a valid email address';
      return;
    }

    // ✅ Validation téléphone optionnel
    if (this.telephone && !this.telephone.match(/^\+216[0-9]{8}$/)) {
      this.errorMessage = 'Phone number must be in format +216XXXXXXXX or leave empty';
      return;
    }

    // ✅ Validation mot de passe
    const passwordRegex = /^(?=.*[A-Z])(?=.*[!@#$%^&*(),.?":{}|<>]).{8,}$/;
    if (!passwordRegex.test(this.password)) {
      this.errorMessage = 'Password must be at least 8 characters, 1 uppercase and 1 symbol';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const formData = new FormData();
    formData.append('firstName', this.firstName);
    formData.append('lastName', this.lastName);
    formData.append('email', this.email);
    formData.append('password', this.password);

    // ✅ Téléphone optionnel
    if (this.telephone && this.telephone.trim() !== '') {
      formData.append('telephone', this.telephone);
    }

    if (this.selectedFile) {
      formData.append('photo', this.selectedFile);
    }

    this.auth.register(formData).subscribe({
      next: (res) => {
        console.log('✅ Register success:', res);
        this.isLoading = false;
        // ✅ Rediriger vers login après inscription
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isLoading = false;
        console.error('❌ Register error - Status:', err.status);
        console.error('❌ Register error - Body:', err.error);

        if (err.status === 400) {
          this.errorMessage = typeof err.error === 'string' ?
            err.error : 'Email already exists';
        } else if (err.status === 500) {
          this.errorMessage = 'Server error. Please try again.';
        } else {
          this.errorMessage = `Error ${err.status}: Please try again`;
        }
      }
    });
  }

  triggerFileInput() {
    const fileInput = document.querySelector('.avatar-circle input') as HTMLInputElement;
    if (fileInput) {
      fileInput.click();
    }
  }

  loginWithGoogle() {
    window.location.href = 'http://localhost:8081/oauth2/authorization/google';
  }
}
