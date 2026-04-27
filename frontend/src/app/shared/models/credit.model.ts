import { CreditStatus } from '../enums/credit-status.enum';
import { Client } from './client.model';
import { AgentFinance } from './agent-finance.model';
import { Admin } from './admin.model';
import { Repayment } from './repayment.model';

export interface Credit {
    creditId: number;
    clientId?: number; // ID du client (exposé par le backend)
    amount: number;
    interestRate: number;
    monthlyPayment: number;
    durationInMonths: number;
    startDate: Date;
    endDate: Date;
    status: CreditStatus;
    dueDate?: Date;
    client?: Client;
    agentFinance?: AgentFinance;
    admin?: Admin;
    repayments?: Repayment[];
}
