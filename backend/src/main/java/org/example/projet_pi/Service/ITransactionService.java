package org.example.projet_pi.Service;

import org.example.projet_pi.entity.Transaction;

import java.util.List;

public interface ITransactionService {
//crud
    /**
     * Ajouter une transaction (dépôt ou retrait) à un compte spécifique
     * @param accountId l'ID du compte
     * @param transaction l'objet Transaction à ajouter
     * @return la Transaction sauvegardée
     */
    Transaction addTransaction(Long accountId, Transaction transaction);

    /**
     * Supprimer une transaction par son ID
     * @param transactionId l'ID de la transaction à supprimer
     */
    void deleteTransaction(Long transactionId);

    /**
     * Récupérer une transaction par son ID
     * @param transactionId l'ID de la transaction
     * @return la Transaction correspondante
     */
    Transaction getTransactionById(Long transactionId);

    /**
     * Récupérer toutes les transactions de la base
     * @return liste de toutes les transactions
     */
    List<Transaction> getAllTransactions();

    /**
     * Récupérer toutes les transactions d’un compte spécifique
     * @param accountId l'ID du compte
     * @return liste des transactions pour ce compte
     */
    List<Transaction> getTransactionsByAccountId(Long accountId);
}
