// src/app/Features/News/Dashboard/dashboard.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

@Component({
    selector: 'app-dashboard-news',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './dashboard-news.component.html',
    styleUrls: ['./dashboard-news.component.css']
})
export class DashboardNewsComponent implements OnInit {

    // ========== STATISTIQUES NEWS ==========
    newsStats = {
        total: 0,
        published: 0,
        draft: 0
    };

    // ========== STATISTIQUES COMPLAINT ==========
    complaintStats = {
        total: 0,
        pending: 0,
        inProgress: 0,
        approved: 0,
        rejected: 0,
        closed: 0,
        resolutionRate: 0,
        rejectionRate: 0,
        averageProcessingTime: 0,
        topAgent: ''
    };

    isLoading = true;
    errorMessage = '';

    constructor(
        private router: Router
    ) {}

    ngOnInit(): void {
        this.loadData();
    }

    loadData(): void {
        this.isLoading = true;

        // Simuler un chargement de données (à remplacer par API réelle)
        setTimeout(() => {
            // Données NEWS
            this.newsStats = {
                total: 8,
                published: 6,
                draft: 2
            };

            // Données COMPLAINT
            this.complaintStats = {
                total: 25,
                pending: 12,
                inProgress: 5,
                approved: 4,
                rejected: 2,
                closed: 2,
                resolutionRate: 16,
                rejectionRate: 8,
                averageProcessingTime: 5.5,
                topAgent: 'Jean Dupont (Assurance)'
            };

            this.isLoading = false;
        }, 1000);
    }

    

    // ⭐ Ajouter la méthode de navigation
    goBackToComplaints(): void {
        console.log('← Navigation retour vers la page des réclamations');
        this.router.navigate(['backoffice/complaint']);
    }

    getFormattedTime(): string {
        const days = this.complaintStats.averageProcessingTime;
        if (days === 0) return 'Non calculé';
        if (days < 1) return '< 1 jour';
        if (days === 1) return '1 jour';
        return `${Math.round(days)} jours`;
    }

    getFormattedRate(rate: number): string {
        if (rate === 0) return '0%';
        return `${rate.toFixed(1)}%`;
    }

    refresh(): void {
        this.loadData();
    }
}