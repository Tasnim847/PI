import { ContractStatus } from '../enums/contract-status.enum';
import { PaymentFrequency } from '../enums/payment-frequency.enum';
import { Client } from './client.model';
import { InsuranceProduct } from './insurance-product.model';
import { AgentAssurance } from './agent-assurance.model';
import { RiskClaim } from './risk-claim.model';
import { Claim } from './claim.model';
import { Payment } from './payment.model';

// insurance-contract.model.ts
export interface InsuranceContract {
    contractId: number;
    startDate: Date;
    endDate: Date;
    premium: number;
    deductible: number;
    coverageLimit: number;
    totalPaid: number;
    remainingAmount: number;
    contractDurationYears: number;
    status: ContractStatus;
    paymentFrequency: PaymentFrequency;
    riskClaim?: RiskClaim;
    client?: Client;
    productId?: number;  // ← Ajouter cette ligne
    product?: InsuranceProduct;  // Garder pour compatibilité
    agentAssurance?: AgentAssurance;
    claims?: Claim[];
    payments?: Payment[];
}
