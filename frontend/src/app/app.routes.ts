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
import { LoginComponent } from './Features/auth/login/login.component';
import { RegisterComponent } from './Features/auth/register/register.component';
import {authGuard} from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
import {ProfileComponent} from './pages/profile/profile.component';
import { AddContractComponent } from './Features/Insurance/pages/client/add-contract/add-contract.component';
import { MyContractsComponent } from './Features/Insurance/pages/client/my-contracts/my-contracts.component';
import { AdminProductListComponent } from './Features/Produit/pages/admin-product-list/admin-product-list/admin-product-list.component';
import { ContractListComponent } from './Features/Insurance/pages/admin/contract-list/contract-list.component';
import { ListAllClaimsComponent } from './Features/Claims/admin/list-all-claims/list-all-claims.component';
import { ListAllCompensationsComponent } from './Features/Compensation/admin/list-all-compensations/list-all-compensations.component';
import { ListMyClaimsComponent } from './Features/Claims/client/list-my-claims/list-my-claims.component';
import { ListMyCompensationsComponent } from './Features/Compensation/client/list-my-compensations/list-my-compensations.component';
export const routes: Routes = [
  // Landing page ouverte par défaut
  { path: '', component: LandingPageComponent },

  // Pages publiques sous NavbarFooterLayout
  {
    path: 'public',
    component: NavbarFooterLayoutComponent,
    children: [
      { path: 'home', component: HomeComponent },
      { path: 'about', component: AboutComponent },
      { path: 'credit', component: CreditPageComponent },
      { path: 'insurance', component: MyContractsComponent },
      { path: 'account', component: AccountPageComponent },
      { path: 'complaint', component: ComplaintPageComponent },
      { path: 'news', component: NewsPageComponent },
      { path: 'products', component: ProductListComponent },
      { path: 'login', component: LoginComponent },
      { 
        path: 'claims', 
        component: ListMyClaimsComponent // 🔐 client connecté
      },
      { 
        path: 'compensation', 
        component: ListMyCompensationsComponent
      },
      { path: 'register', component: RegisterComponent },
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
      
      
    ]
  },

  // Pages protégées sous SidebarLayout (dashboard)
  {
    path: 'backoffice',
    component: SidebarLayoutComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
    children: [
      { path: '', component: DashboardComponent },
      { path: 'products', component: AdminProductListComponent },
      { path: 'credit', component: CreditPageComponent },
      { path: 'insurance', component: ContractListComponent },
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
