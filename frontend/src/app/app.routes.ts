import { Routes } from '@angular/router';
import { LandingPageComponent } from './pages/landing-page/landing-page.component';
import { NavbarFooterLayoutComponent } from './layouts/navbar-footer-layout/navbar-footer-layout.component';
import { SidebarLayoutComponent } from './layouts/sidebar-layout/sidebar-layout.component';
import { HomeComponent } from './pages/home/home.component';
import { AboutComponent } from './pages/about/about.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { ProductListComponent } from './Features/Produit/pages/product-list/product-list.component';
import { CreditPageComponent } from './Features/Credit/pages/credit-page/credit-page.component';
import { AccountPageComponent } from './Features/Account/pages/account-page/account-page.component';
import { ComplaintPageComponent } from './Features/Complaint/pages/complaint-page/complaint-page.component';
import { NewsPageComponent } from './Features/News/pages/news-page/news-page.component';
import {authGuard} from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
import {ProfileComponent} from './pages/profile/profile.component';
import { AddContractComponent } from './Features/Insurance/pages/client/add-contract/add-contract.component';
import { MyContractsComponent } from './Features/Insurance/pages/client/my-contracts/my-contracts.component';
import { AdminProductListComponent } from './Features/Produit/pages/admin-product-list/admin-product-list/admin-product-list.component';
import { ContractListComponent } from './Features/Insurance/pages/admin/contract-list/contract-list.component';
import { ListAllClaimsComponent } from './Features/Claims/admin/list-all-claims/list-all-claims.component';
import { ListMyClaimsComponent } from './Features/Claims/client/list-my-claims/list-my-claims.component';
import { AddClaimComponent } from './Features/Claims/client/add-claim/add-claim.component';
import {ForgotPasswordComponent} from './Features/auth/forgot-password/forgot-password.component';
import { UserManagementComponent } from './pages/dashboard/user-management/user-management.component';
import {DashboardProfileComponent} from './pages/dashboard-profile/dashboard-profile.component';
import { InsuranceRouterComponent } from './Features/Insurance/insurance-router';
import { AgentClaimsComponent } from './Features/Claims/agent/agent-claims/agent-claims.component';
import { AgentContractsComponent } from './Features/Insurance/pages/agent/agent-contracts/agent-contracts.component';
import { ListMyCompensationsComponent } from './Features/Compensation/client/list-my-compensations/list-my-compensations.component';
import { ListAllCompensationsComponent } from './Features/Compensation/admin/list-all-compensations/list-all-compensations.component';
import { ContractRiskDetailsComponent } from './Features/Insurance/pages/admin/contract-risk-details/contract-risk-details.component';
import { AdminDashboardComponent } from './Features/Insurance/pages/admin/admin-dashboard/admin-dashboard.component';
import { ClientRepaymentComponent } from './Features/Credit/pages/repayment/client-repayment/client-repayment.component'; 


export const routes: Routes = [
  // Landing page ouverte par défaut
  { path: '', component: LandingPageComponent },

  // Routes pour login et register (DOIVENT ÊTRE DÉCOMMENTÉES)
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

  // Pages publiques sous NavbarFooterLayout
  {
    path: 'public',
    component: NavbarFooterLayoutComponent,
    children: [
      { path: 'home', component: HomeComponent },
      { path: 'about', component: AboutComponent },
      { path: 'credit', component: CreditPageComponent },
      { path: 'repayment/:id', component: ClientRepaymentComponent, canActivate: [authGuard] },
      { path: 'insurance', component: InsuranceRouterComponent }, // ✅ MODIFIÉ ICI
      { path: 'account', component: AccountPageComponent },
      { path: 'complaint', component: ComplaintPageComponent },
      { path: 'news', component: NewsPageComponent },
      { path: 'products', component: ProductListComponent },
      {path: 'profile', component: ProfileComponent},
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
    
    // Route pour l'agent (gardée séparément)
    { 
      path: 'agent/contracts', 
      component: AgentContractsComponent,
      canActivate: [roleGuard],
      data: { roles: ['AGENT_ASSURANCE'] }
    },
    
    // Routes pour les claims
    {
      path: 'claims',
      component: ListMyClaimsComponent,
      canActivate: [authGuard],
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
      canActivate: [authGuard]
    },

    ]
  },

  /*
  {
    path: 'login',
    component: LandingPageComponent,
    children: []
  },
  {
    path: 'register',
    component: LandingPageComponent,
    children: []
  },
  */


  // Pages protégées sous SidebarLayout (dashboard)
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
      { path: 'insurance', component: ContractListComponent },
      { path: 'insurance/contract-risk/:id', component: ContractRiskDetailsComponent }, // ✅ Ajoutez cette ligne
      { path: 'insurance/admin-dashboard', component: AdminDashboardComponent }, // ✅ Route dashboard
      { path: 'account', component: AccountPageComponent },
      { path: 'complaint', component: ComplaintPageComponent },
      { path: 'news', component: NewsPageComponent },
      { path: 'claims', component: ListAllClaimsComponent },
      { path: 'compensation', component: ListAllCompensationsComponent }

    ]
  },

  // Redirection si chemin inconnu
  { path: '**', redirectTo: '' }
];
/**************/
