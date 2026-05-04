import { Routes } from '@angular/router';
import { LandingPageComponent } from './pages/landing-page/landing-page.component';
import { NavbarFooterLayoutComponent } from './layouts/navbar-footer-layout/navbar-footer-layout.component';
import { SidebarLayoutComponent } from './layouts/sidebar-layout/sidebar-layout.component';
import { HomeComponent } from './pages/home/home.component';
import { AboutComponent } from './pages/about/about.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { ProductListComponent } from './Features/Produit/pages/product-list/product-list.component';
import { CreditPageComponent } from './Features/Credit/credit-page/pages/credit-page/credit-page.component';
import { AccountPageComponent } from './Features/Account/pages/account-page/account-page.component';
import { ComplaintPageComponent } from './Features/Complaint/pages/complaint-page/complaint-page.component';
import { NewsPageComponent } from './Features/News/pages/news-page/news-page.component';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
import { ProfileComponent } from './pages/profile/profile.component';
import { AddContractComponent } from './Features/Insurance/pages/client/add-contract/add-contract.component';
import { MyContractsComponent } from './Features/Insurance/pages/client/my-contracts/my-contracts.component';
import { AdminProductListComponent } from './Features/Produit/pages/admin-product-list/admin-product-list/admin-product-list.component';
import { ContractListComponent } from './Features/Insurance/pages/admin/contract-list/contract-list.component';
import { ListAllClaimsComponent } from './Features/Claims/admin/list-all-claims/list-all-claims.component';
import { ListMyClaimsComponent } from './Features/Claims/client/list-my-claims/list-my-claims.component';
import { AddClaimComponent } from './Features/Claims/client/add-claim/add-claim.component';
import { ForgotPasswordComponent } from './Features/auth/forgot-password/forgot-password.component';
import { UserManagementComponent } from './pages/dashboard/user-management/user-management.component';
import { DashboardProfileComponent } from './pages/dashboard-profile/dashboard-profile.component';
import { OauthCallbackComponent } from './Features/auth/oauth-callback.component';
import { ClientRepaymentComponent } from './Features/Credit/credit-page/pages/repayment/client-repayment/client-repayment.component';
import { InsuranceRouterComponent } from './Features/Insurance/insurance-router';
import { ProductDetailComponent } from './Features/Produit/pages/product-detail/product-detail.component';
import { DashboardInsuranceComponent } from './Features/Insurance/pages/client/dashboard-insurance/dashboard-insurance.component';
import { PaymentPageComponent } from './Features/Insurance/pages/client/payment-page/payment-page.component';
import { AgentContractsComponent } from './Features/Insurance/pages/agent/agent-contracts/agent-contracts.component';
import { AgentClaimsComponent } from './Features/Claims/agent/agent-claims/agent-claims.component';
import { ListMyCompensationsComponent } from './Features/Compensation/client/list-my-compensations/list-my-compensations.component';
import { AdminRepaymentComponent } from './Features/Credit/credit-page/pages/repayment/admin-repayment/admin-repayment.component';
import { ContractRiskDetailsComponent } from './Features/Insurance/pages/admin/contract-risk-details/contract-risk-details.component';
import { AdminDashboardComponent } from './Features/Insurance/pages/admin/admin-dashboard/admin-dashboard.component';
import { ClaimsDashboardComponent } from './Features/Claims/admin/claims-dashboard/claims-dashboard.component';
import { ListAllCompensationsComponent } from './Features/Compensation/admin/list-all-compensations/list-all-compensations.component';
import { ComplaintKpiComponent } from './Features/Complaint/pages/complaint-kpi/complaint-kpi.component';
import { TransactionPageComponent } from './Features/Transaction/pages/transaction-page/transaction-page.component';
import { MyComplaintsComponent } from './Features/Complaint/pages/my-complaints/my-complaints.component';
import { AgentComplaintsComponent } from './Features/Complaint/pages/agent-complaints/agent-complaints.component';
import { ClientDashboardComponent } from './components/client-dashboard/client-dashboard.component';
// ============================================
// 🆕 IMPORTS DES NOUVEAUX COMPOSANTS
// ============================================
import { ClientAccountsComponent } from './components/client-accounts/client-accounts.component';
import { TransferByRipComponent } from './components/transfer-by-rip/transfer-by-rip.component';

import { AgentAccountComponent } from './components/agent-account/agent-account.component';
import { AgentCashApprovalsComponent } from './Features/Insurance/pages/agent/agent-cash-approvals/agent-cash-approvals.component';
import { AgentCompensationListComponent } from './Features/Compensation/agent/agent-compensation-list/agent-compensation-list.component';
import { HomeAgentComponent } from './Features/Home agent/home-agent/home-agent.component';
import { AdminCompensationDetailsComponent } from './Features/Compensation/admin/admin-compensation-details/admin-compensation-details.component';
import { CompensationDetailsComponent } from './Features/Compensation/client/compensation-details/compensation-details.component';
import { InssAgentDashboardComponent } from './Features/Insurance/pages/agent/inss-agent-dashboard/inss-agent-dashboard.component';
import { AgentCreditApprovalsComponent } from './Features/Credit/credit-page/pages/agent/agent-credit-approvals.component';
import { CompAgentDashboardComponent } from './Features/Compensation/agent/comp-agent-dashboard/comp-agent-dashboard.component';
import { AgentClaimsDetailsComponent } from './Features/Claims/agent/agent-claims-details/agent-claims-details.component';
import { DashboardNewsComponent } from './Features/Complaint/pages/Dashboard/dashboard-news.component';

export const routes: Routes = [

  // Landing page ouverte par défaut
  { path: '', component: LandingPageComponent },

  // Routes pour login et register
  {
    path: 'login',
    component: LandingPageComponent
  },
  {
    path: 'register',
    component: LandingPageComponent
  },
  {
    path: 'forgot-password',
    component: LandingPageComponent
  },
  
  // Route OAuth2 Callback
  {
    path: 'oauth2/callback',
    component: OauthCallbackComponent
  },
  {
    path: 'face-login',
    loadComponent: () => import('./components/face-login/face-login.component')
      .then(m => m.FaceLoginComponent)
  },
  {
    path: 'face-register',
    loadComponent: () => import('./components/face-register/face-register.component')
      .then(m => m.FaceRegisterComponent)
  },

  // ============================================
  // PAGES AVEC NAVBAR ET FOOTER (Clients + Agents)
  // ============================================
  {
    path: 'public',
    component: NavbarFooterLayoutComponent,
    children: [
      // Pages publiques
      { path: 'home', component: HomeComponent },
      { path: 'about', component: AboutComponent },
      { path: 'credit', component: CreditPageComponent },
      { path: 'repayment/:id', component: ClientRepaymentComponent, canActivate: [authGuard] },
      { path: 'insurance', component: InsuranceRouterComponent },
      { path: 'account', component: AccountPageComponent },
      { path: 'complaint', component: ComplaintPageComponent },
      { path: 'complaint/kpi', component: ComplaintKpiComponent },
      { path: 'news', component: NewsPageComponent },
      { path: 'products', component: ProductListComponent },
      { path: 'product-detail/:id', component: ProductDetailComponent },
      { path: 'profile', component: ProfileComponent },
      { path: 'agent/home', component: HomeAgentComponent, canActivate: [roleGuard], data: { roles: ['AGENT_ASSURANCE'] } },


      // 🆕 ROUTES CLIENT - NOUVELLES FONCTIONNALITÉS


      {
        path: 'client/dashboard',
        component: ClientDashboardComponent,
        canActivate: [roleGuard],
        data: { roles: ['CLIENT'] }
      },
      {
        path: 'client/accounts',
        component: ClientAccountsComponent,
        canActivate: [roleGuard],
        data: { roles: ['CLIENT'] }
      },
      {
        path: 'agent/account',
        component: AgentAccountComponent,
        canActivate: [roleGuard],
        data: { roles: ['AGENT_FINANCE'] }
      },
      {
        path: 'client/transfer',
        component: TransferByRipComponent,
        canActivate: [roleGuard],
        data: { roles: ['CLIENT'] }
      },
      
      // 🆕 ROUTES AGENT_FINANCE
      {
        path: 'agent/credit-approvals',
        component: AgentCreditApprovalsComponent,
        canActivate: [roleGuard],
        data: { roles: ['AGENT_FINANCE'] }
      },
      
      // Insurance routes (Client)
      { 
        path: 'insurance/add-contract', 
        component: AddContractComponent,
        canActivate: [authGuard]
      },
      { 
        path: 'insurance/my-contracts', 
        component: MyContractsComponent,
        canActivate: [authGuard]
      }, 
      { 
        path: 'insurance/dashboard', 
        component: DashboardInsuranceComponent,
        canActivate: [authGuard]
      }, 
      { 
        path: 'insurance/payment/:id', 
        component: PaymentPageComponent,
        canActivate: [authGuard]
      },
      
      // Route pour l'agent (Insurance)
      { 
        path: 'agent/contracts', 
        component: AgentContractsComponent,
        canActivate: [roleGuard],
        data: { roles: ['AGENT_ASSURANCE'] }
      },
      { path: 'agent/dashboard', 
        component: InssAgentDashboardComponent,
        canActivate: [roleGuard],
        data: { roles: ['AGENT_ASSURANCE'] } 
      },
      {
        path: 'agent/dashb/:id',
        component: CompAgentDashboardComponent,
        canActivate: [roleGuard],
        data: { roles: ['AGENT_ASSURANCE'] } 
      },
      {
        path: 'agent/dashb',
        component: CompAgentDashboardComponent,
        canActivate: [roleGuard],
        data: { roles: ['AGENT_ASSURANCE'] } 
      },
      {
        path: 'agent/cash-approvals',
        component: AgentCashApprovalsComponent,
        canActivate: [roleGuard],
        data: { roles: ['AGENT_ASSURANCE'] }
      },
      {
        path: 'agent/claims/:id',
        component: AgentClaimsDetailsComponent
      },
      
      // Routes pour les claims
      {
        path: 'claims',
        component: ListMyClaimsComponent,
        canActivate: [roleGuard],
        data: { roles: ['CLIENT'] }
      },
      {
        path: 'agent/claims',
        component: AgentClaimsComponent,
        canActivate: [roleGuard],
        data: { roles: ['AGENT_ASSURANCE'] }
      },
      { 
        path: 'claims/new', 
        component: AddClaimComponent,
        canActivate: [authGuard]
      },
      { 
        path: 'compensations', 
        component: ListMyCompensationsComponent,
        canActivate: [roleGuard],
        data: { roles: ['CLIENT'] }
      },
      { 
        path: 'compensations/:id/details', 
        component: CompensationDetailsComponent,
        canActivate: [roleGuard],
        data: { roles: ['CLIENT'] }
      },
      { 
        path: 'agent/compensations', 
        component: AgentCompensationListComponent,
        canActivate: [roleGuard],
        data: { roles: ['AGENT_ASSURANCE'] }
      },
      
      // Routes pour les réclamations
      {
        path: 'my-complaints',
        component: MyComplaintsComponent,
        canActivate: [roleGuard],
        data: { roles: ['CLIENT'] }
      },
      {
        path: 'agent/complaints',
        component: AgentComplaintsComponent,
        canActivate: [roleGuard],
        data: { roles: ['AGENT_ASSURANCE', 'AGENT_FINANCE'] }
      }
    ]
  },

  // ============================================
  // BACKOFFICE ADMIN (Sans navbar/footer, avec sidebar)
  // ============================================
  {
    path: 'backoffice',
    component: SidebarLayoutComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
    children: [
      { path: '', component: DashboardComponent },
      { path: 'clients', component: UserManagementComponent, data: { type: 'clients' } },
      { path: 'agents-assurance', component: UserManagementComponent, data: { type: 'agents-assurance' } },
      { path: 'agents-finance', component: UserManagementComponent, data: { type: 'agents-finance' } },
      { path: 'admins', component: UserManagementComponent, data: { type: 'admins' } },
      { path: 'profile', component: DashboardProfileComponent },
      { path: 'products', component: AdminProductListComponent },
      { path: 'credit', component: CreditPageComponent },
      { path: 'repayment', component: AdminRepaymentComponent },
      { path: 'insurance', component: ContractListComponent },
      { path: 'insurance/contract-risk/:id', component: ContractRiskDetailsComponent },
      { path: 'insurance/admin-dashboard', component: AdminDashboardComponent },
      { path: 'account', component: AccountPageComponent },
      { path: 'complaint', component: ComplaintPageComponent },
      { path: 'dashboard/kpi', component: DashboardNewsComponent },
      { path: 'news', component: NewsPageComponent },
      { path: 'claims', component: ListAllClaimsComponent },
      {
        path: 'claims-dashboard',
        component: ClaimsDashboardComponent,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] }
      },
      { path: 'compensation', component: ListAllCompensationsComponent },
      { path: 'compensation/:id', component: AdminCompensationDetailsComponent },
      { path: 'transaction', component: TransactionPageComponent }
    ]
  },

  // Redirection si chemin inconnu
  { path: '**', redirectTo: '' }
];