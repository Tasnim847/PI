// src/app/Features/News/pages/news-page/news-page.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NewsService } from '../../../../services/news.service';
import { AuthService } from '../../../../services/auth.service';
import { Router } from '@angular/router';
import { News, NewsStatus } from '../../../../shared/models/news.model';

@Component({
    selector: 'app-news-page',
    standalone: true,
    imports: [CommonModule, FormsModule, ReactiveFormsModule],
    templateUrl: './news-page.component.html',
    styleUrls: ['./news-page.component.css']
})
export class NewsPageComponent implements OnInit {

    newsList: News[] = [];
    selectedNews: News | null = null;
    newsForm: FormGroup;
    isEditing = false;
    showForm = false;
    isLoading = false;
    errorMessage = '';
    successMessage = '';
    searchTerm = '';
    selectedCategory = '';
    selectedStatus = '';

    currentUserRole: string | null = null;

    categories = ['TECHNOLOGIE', 'FINANCE', 'ASSURANCE', 'ECONOMIE', 'POLITIQUE', 'SPORT', 'DIVERS'];
    statusOptions = Object.values(NewsStatus);

    constructor(
        private newsService: NewsService,
        private authService: AuthService,
        private router: Router,
        private fb: FormBuilder
    ) {
        this.newsForm = this.createForm();
    }

// news-page.component.ts
ngOnInit(): void {
    // Vérifier si on est dans le navigateur
    if (typeof window !== 'undefined') {
        console.log('🔍 === DIAGNOSTIC ===');
        console.log('Token:', localStorage.getItem('token'));
        console.log('Rôle:', localStorage.getItem('role'));
        console.log('User:', localStorage.getItem('user'));
        this.checkAuthentication();
    } else {
        // Côté serveur, ne pas faire d'appels API
        console.log('⚠️ Exécution côté serveur');
    }
}

checkAuthentication(): void {
    if (typeof window === 'undefined') return;
    
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');
    
    console.log('🔍 checkAuthentication - Token:', token ? 'PRÉSENT' : 'ABSENT');
    console.log('🔍 checkAuthentication - Rôle:', role);
    
    this.currentUserRole = role;

    if (!token) {
        this.errorMessage = 'Veuillez vous connecter';
        setTimeout(() => this.router.navigate(['/login']), 2000);
        return;
    }

    this.loadNews();
}

    private createForm(): FormGroup {
        return this.fb.group({
            title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(200)]],
            content: ['', [Validators.required, Validators.minLength(20)]],
            summary: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
            author: ['', Validators.required],
            category: ['', Validators.required],
            status: [NewsStatus.DRAFT, Validators.required]
        });
    }

    loadNews(): void {
        console.log('🔍 loadNews - Appel API');
        this.isLoading = true;
        this.newsService.getAllNews().subscribe({
            next: (data: News[]) => {
                console.log('✅ News chargées:', data.length);
                this.newsList = data;
                this.isLoading = false;
            },
            error: (err: any) => {
                console.error('❌ Erreur chargement:', err.status, err.message);
                if (err.status === 403) {
                    this.errorMessage = 'Accès non autorisé - Rôle requis: ADMIN';
                } else if (err.status === 401) {
                    this.errorMessage = 'Non authentifié. Veuillez vous reconnecter.';
                } else {
                    this.errorMessage = 'Erreur lors du chargement';
                }
                this.isLoading = false;
                setTimeout(() => this.errorMessage = '', 3000);
            }
        });
    }

    get filteredNews(): News[] {
        let filtered = this.newsList;

        if (this.searchTerm) {
            const term = this.searchTerm.toLowerCase();
            filtered = filtered.filter(n =>
                n.title.toLowerCase().includes(term) ||
                n.author.toLowerCase().includes(term) ||
                n.category.toLowerCase().includes(term)
            );
        }

        if (this.selectedCategory) {
            filtered = filtered.filter(n => n.category === this.selectedCategory);
        }

        if (this.selectedStatus) {
            filtered = filtered.filter(n => n.status === this.selectedStatus);
        }

        return filtered;
    }

    openAddForm(): void {
        this.isEditing = false;
        this.showForm = true;
        this.selectedNews = null;
        
        const userStr = localStorage.getItem('user');
        let defaultAuthor = 'Admin';
        if (userStr) {
            try {
                const user = JSON.parse(userStr);
                defaultAuthor = user.firstName ? `${user.firstName} ${user.lastName}` : 'Admin';
            } catch(e) {}
        }
        
        this.newsForm.reset({
            title: '',
            content: '',
            summary: '',
            author: defaultAuthor,
            category: '',
            status: NewsStatus.DRAFT
        });
    }

    editNews(news: News, event: Event): void {
        event.stopPropagation();
        this.isEditing = true;
        this.showForm = true;
        this.selectedNews = news;
        this.newsForm.patchValue({
            title: news.title,
            content: news.content,
            summary: news.summary,
            author: news.author,
            category: news.category,
            status: news.status
        });
    }

    viewDetails(news: News): void {
        this.selectedNews = news;
    }

    closeDetails(): void {
        this.selectedNews = null;
    }

    saveNews(): void {
        console.log('🔍 saveNews - Formulaire valide:', this.newsForm.valid);
        console.log('Valeurs:', this.newsForm.value);
        
        if (this.newsForm.invalid) {
            Object.keys(this.newsForm.controls).forEach(key => {
                const control = this.newsForm.get(key);
                if (control) control.markAsTouched();
            });
            return;
        }

        this.isLoading = true;
        const formValue = this.newsForm.value;

        if (this.isEditing && this.selectedNews) {
            const updateData = {
                title: formValue.title,
                content: formValue.content,
                summary: formValue.summary,
                author: formValue.author,
                category: formValue.category,
                status: formValue.status
            };
            
            this.newsService.updateNews(this.selectedNews.newsId, updateData).subscribe({
                next: (response) => {
                    console.log('✅ Modification réussie:', response);
                    this.successMessage = 'Actualité modifiée avec succès !';
                    this.afterSave();
                },
                error: (err: any) => {
                    console.error('❌ Erreur modification:', err);
                    this.handleError('Erreur lors de la modification');
                }
            });
        } else {
            const createData = {
                title: formValue.title,
                content: formValue.content,
                summary: formValue.summary,
                author: formValue.author,
                category: formValue.category,
                status: formValue.status
            };
            
            console.log('📤 Envoi création:', createData);
            
            this.newsService.createNews(createData).subscribe({
                next: (response) => {
                    console.log('✅ Création réussie:', response);
                    this.successMessage = 'Actualité créée avec succès !';
                    this.afterSave();
                },
                error: (err: any) => {
                    console.error('❌ Erreur création:', err);
                    if (err.status === 403) {
                        this.handleError('Accès non autorisé. Vous devez être ADMIN.');
                    } else {
                        this.handleError('Erreur lors de la création');
                    }
                }
            });
        }
    }

    deleteNews(id: number, event: Event): void {
        event.stopPropagation();
        if (confirm('Supprimer cette actualité ?')) {
            this.isLoading = true;
            this.newsService.deleteNews(id).subscribe({
                next: () => {
                    this.successMessage = 'Actualité supprimée !';
                    this.loadNews();
                    this.isLoading = false;
                    if (this.selectedNews?.newsId === id) this.closeDetails();
                    setTimeout(() => this.successMessage = '', 3000);
                },
                error: (err: any) => {
                    console.error(err);
                    this.handleError('Erreur lors de la suppression');
                }
            });
        }
    }

    publishNews(id: number, event: Event): void {
        event.stopPropagation();
        this.newsService.publishNews(id).subscribe({
            next: () => {
                this.successMessage = 'Actualité publiée !';
                this.loadNews();
                setTimeout(() => this.successMessage = '', 3000);
            },
            error: (err: any) => {
                console.error(err);
                this.handleError('Erreur lors de la publication');
            }
        });
    }

    archiveNews(id: number, event: Event): void {
        event.stopPropagation();
        this.newsService.archiveNews(id).subscribe({
            next: () => {
                this.successMessage = 'Actualité archivée !';
                this.loadNews();
                setTimeout(() => this.successMessage = '', 3000);
            },
            error: (err: any) => {
                console.error(err);
                this.handleError('Erreur lors de l\'archivage');
            }
        });
    }

    afterSave(): void {
        setTimeout(() => {
            this.loadNews();
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
        this.selectedNews = null;
    }

    resetFilters(): void {
        this.searchTerm = '';
        this.selectedCategory = '';
        this.selectedStatus = '';
    }

    getStatusLabel(status: string): string {
        const labels: Record<string, string> = {
            'DRAFT': '📝 Brouillon',
            'PUBLISHED': '✅ Publié',
            'ARCHIVED': '📦 Archivé'
        };
        return labels[status] || status;
    }

    getStatusClass(status: string): string {
        const classes: Record<string, string> = {
            'DRAFT': 'status-draft',
            'PUBLISHED': 'status-published',
            'ARCHIVED': 'status-archived'
        };
        return classes[status] || '';
    }

    getStatusIcon(status: string): string {
        const icons: Record<string, string> = {
            'DRAFT': '📝',
            'PUBLISHED': '✅',
            'ARCHIVED': '📦'
        };
        return icons[status] || '📰';
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

    truncateText(text: string, maxLength: number): string {
        if (!text) return '';
        return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    }

    canEdit(): boolean {
        return this.currentUserRole === 'ADMIN' || this.currentUserRole === 'AGENT_ASSURANCE';
    }
}