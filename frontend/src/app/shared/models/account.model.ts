import { AccountType } from '../enums/account-type.enum';
import { Client } from './client.model';
import { Transaction } from './transaction.model';

export interface Account {
    accountId: number;
    balance: number;
    type: AccountType;
    status: string;
    dailyLimit: number;
    monthlyLimit: number;
    client?: Client;
    transactions?: Transaction[];
}
