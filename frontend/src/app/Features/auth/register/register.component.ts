import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterModule], // Ajout de RouterModule pour routerLink
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
  selectedFile: File | null = null; // Changé de selectedFile! à selectedFile: File | null = null
  errorMessage: string = ''; // Changé de protected errorMessage: any à string = ''
  isLoading: boolean = false; // Ajouté pour gérer le chargement

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
    // Validation des champs
    if (!this.firstName || !this.lastName || !this.email || !this.password || !this.telephone) {
      this.errorMessage = 'Please fill in all fields';
      return;
    }

    // Validation email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.email)) {
      this.errorMessage = 'Please enter a valid email address';
      return;
    }

    // Validation téléphone
    const phoneRegex = /^\+216[0-9]{8}$/;
    if (!phoneRegex.test(this.telephone)) {
      this.errorMessage = 'Please enter a valid phone number (+216XXXXXXXX)';
      return;
    }

    // Validation mot de passe (minimum 6 caractères)
    if (this.password.length < 6) {
      this.errorMessage = 'Password must be at least 6 characters';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const formData = new FormData();
    formData.append('firstName', this.firstName);
    formData.append('lastName', this.lastName);
    formData.append('email', this.email);
    formData.append('password', this.password);
    formData.append('telephone', this.telephone);

    if (this.selectedFile) {
      formData.append('photo', this.selectedFile);
    }

    this.auth.register(formData).subscribe({
      next: (res) => {
        console.log("REGISTER RESPONSE:", res);
        localStorage.setItem('token', res.token); // si backend renvoie token
        this.router.navigate(['/public/home']);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Registration failed. Please try again.';
        console.error('Registration error:', err);
      }
    });
  }

  triggerFileInput() {
    const fileInput = document.querySelector('.avatar-circle input') as HTMLInputElement;
    if (fileInput) {
      fileInput.click();
    }
  }
}
