// src/app/shared/dto/account-statistics.dto.ts
export interface AccountStatisticsDTO {
  accountId: number;
  totalDeposits: number;
  totalWithdrawals: number;
  currentBalance: number;
  averageTransactionAmount: number;
  totalTransactions: number;
  totalDepositCount: number;
  totalWithdrawalCount: number;
}