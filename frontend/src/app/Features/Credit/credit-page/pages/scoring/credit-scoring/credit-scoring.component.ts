import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScoringService, CreditScore, ScoringResponse } from '../../../../services/scoring.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-credit-scoring',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './credit-scoring.component.html',
  styleUrl: './credit-scoring.component.css'
})
export class CreditScoringComponent implements OnInit {
  @Input() clientId: number | null = null;
  @Input() clientName: string = '';
  @Input() showFullAnalysis: boolean = true;
  @Output() scoreCalculated = new EventEmitter<CreditScore>();

  // ========== DATA ==========
  creditScore: CreditScore | null = null;
  isLoading: boolean = false;
  isAnalyzing: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  constructor(
    private scoringService: ScoringService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    if (this.clientId) {
      this.calculateScore();
    }
  }

  // ========== CALCULATE SCORE ==========
  calculateScore(): void {
    if (!this.clientId) {
      this.errorMessage = 'Client ID required';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.scoringService.calculateCreditScore(this.clientId).subscribe({
      next: (response: ScoringResponse) => {
        if (response.success && response.data) {
          this.creditScore = response.data;
          this.successMessage = 'Score calculated successfully';
          if (this.creditScore) {
            this.scoreCalculated.emit(this.creditScore);
          }
          this.toastr.success('Credit score calculated');
        } else {
          this.errorMessage = response.message || 'Error during calculation';
          this.toastr.error(this.errorMessage);
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Score calculation error:', err);
        this.errorMessage = err.error?.message || 'Error calculating credit score';
        this.toastr.error(this.errorMessage);
        this.isLoading = false;
      }
    });
  }

  // ========== ANALYZE WITH GEMINI ==========
  analyzeWithGemini(): void {
    if (!this.clientId) {
      this.errorMessage = 'Client ID required';
      return;
    }

    this.isAnalyzing = true;
    this.errorMessage = '';

    this.scoringService.analyzeClientProfile(this.clientId).subscribe({
      next: (response: ScoringResponse) => {
        if (response.success) {
          // Update with new data
          if (response.data) {
            this.creditScore = response.data;
          } else {
            // Build object from response
            this.creditScore = {
              clientId: response.clientId || this.clientId!,
              clientName: this.clientName,
              clientEmail: '',
              score: response.score || 0,
              riskLevel: response.riskLevel || '',
              recommendation: response.recommendation || '',
              analysis: response.analysis || '',
              calculatedAt: response.calculatedAt || new Date().toISOString(),
              ...response.metrics
            };
          }
          
          this.successMessage = 'AI analysis completed';
          if (this.creditScore) {
            this.scoreCalculated.emit(this.creditScore);
          }
          this.toastr.success('Gemini AI analysis completed');
        } else {
          this.errorMessage = response.message || 'Error during analysis';
          this.toastr.error(this.errorMessage);
        }
        this.isAnalyzing = false;
      },
      error: (err) => {
        console.error('Gemini analysis error:', err);
        this.errorMessage = err.error?.message || 'Error during AI analysis';
        this.toastr.error(this.errorMessage);
        this.isAnalyzing = false;
      }
    });
  }

  // ========== UTILITIES ==========
  getRiskLevelClass(riskLevel: string): string {
    return this.scoringService.getRiskLevelClass(riskLevel);
  }

  getRiskLevelLabel(riskLevel: string): string {
    return this.scoringService.getRiskLevelLabel(riskLevel);
  }

  getRecommendationClass(recommendation: string): string {
    return this.scoringService.getRecommendationClass(recommendation);
  }

  getRecommendationLabel(recommendation: string): string {
    return this.scoringService.getRecommendationLabel(recommendation);
  }

  getScoreColor(score: number): string {
    return this.scoringService.getScoreColor(score);
  }

  getScoreLabel(score: number): string {
    return this.scoringService.getScoreLabel(score);
  }

  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'TND'
    }).format(amount);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatPercentage(value: number): string {
    return `${value.toFixed(1)}%`;
  }

  // ========== ACTIONS ==========
  refreshScore(): void {
    this.calculateScore();
  }

  exportScore(): void {
    if (!this.creditScore) return;

    const data = {
      client: this.creditScore.clientName,
      score: this.creditScore.score,
      riskLevel: this.getRiskLevelLabel(this.creditScore.riskLevel),
      recommendation: this.getRecommendationLabel(this.creditScore.recommendation),
      analysis: this.creditScore.analysis,
      calculatedAt: this.formatDate(this.creditScore.calculatedAt)
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `credit_score_${this.creditScore.clientId}_${Date.now()}.json`;
    link.click();
    window.URL.revokeObjectURL(url);

    this.toastr.success('Score exported successfully');
  }
}