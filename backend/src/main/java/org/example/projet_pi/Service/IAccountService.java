package org.example.projet_pi.Service;

import org.example.projet_pi.entity.Account;

import java.util.List;

//crud
public interface IAccountService {

    // 🔹 Ajouter un compte
    Account addAccount(Account account);

    // 🔹 Mettre à jour un compte
    Account updateAccount(Account account);

    // 🔹 Supprimer un compte
    void deleteAccount(Long id);

    // 🔹 Récupérer un compte par ID
    Account getAccountById(Long id);

    // 🔹 Récupérer tous les comptes
    List<Account> getAllAccounts();

    // 🔹 Récupérer les comptes par ID client
    List<Account> getAccountsByClientId(Long clientId);

    // 🔹 Récupérer les comptes par statut
    List<Account> getAccountsByStatus(String status);
}
