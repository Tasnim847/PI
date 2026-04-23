import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService } from '../../../../services/account.service';
import { Account } from '../../../../shared/models/account.model';
import { AccountType } from '../../../../shared/enums/account-type.enum';

@Component({
  selector: 'app-account-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './account-page.component.html',
  styleUrls: ['./account-page.component.css']
})
export class AccountPageComponent implements OnInit {

  accounts: Account[] = [];
  selectedAccount: Account | null = null;
  isFormVisible = false;
  isEditMode = false;

  // ✅ expose l'enum au template HTML
  AccountType = AccountType;

  formData: Partial<Account> = {
    balance: 0,
    type: AccountType.SAVINGS,
    status: 'ACTIVE',
    dailyLimit: 0,
    monthlyLimit: 0
  };

  constructor(private accountService: AccountService) {}

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.accountService.getAllAccounts().subscribe({
      next: (data) => this.accounts = data,
      error: (err) => console.error('Erreur chargement comptes', err)
    });
  }

  openCreateForm(): void {
    this.isEditMode = false;
    this.formData = {
      balance: 0,
      type: AccountType.SAVINGS,
      status: 'ACTIVE',
      dailyLimit: 0,
      monthlyLimit: 0
    };
    this.isFormVisible = true;
  }

  openEditForm(account: Account): void {
    this.isEditMode = true;
    this.formData = { ...account };
    this.selectedAccount = account;
    this.isFormVisible = true;
  }

  submitForm(): void {
    if (this.isEditMode && this.selectedAccount?.accountId) {
      this.accountService.updateAccount(
        this.selectedAccount.accountId,
        this.formData as Account
      ).subscribe({
        next: () => {
          this.loadAccounts();
          this.isFormVisible = false;
        },
        error: (err) => console.error('Erreur modification', err)
      });
    } else {
      this.accountService.createAccount(this.formData as Account).subscribe({
        next: () => {
          this.loadAccounts();
          this.isFormVisible = false;
        },
        error: (err) => console.error('Erreur création', err)
      });
    }
  }

  deleteAccount(accountId: number): void {
    if (confirm('Confirmer la suppression ?')) {
      this.accountService.deleteAccount(accountId).subscribe({
        next: () => this.loadAccounts(),
        error: (err) => console.error('Erreur suppression', err)
      });
    }
  }

  setLimits(account: Account): void {
    const daily = parseFloat(
      prompt('Daily limit ?', String(account.dailyLimit)) || '0'
    );
    const monthly = parseFloat(
      prompt('Monthly limit ?', String(account.monthlyLimit)) || '0'
    );
    this.accountService.setLimits(account.accountId, daily, monthly).subscribe({
      next: () => this.loadAccounts(),
      error: (err) => console.error('Erreur limites', err)
    });
  }

  cancelForm(): void {
    this.isFormVisible = false;
  }
}