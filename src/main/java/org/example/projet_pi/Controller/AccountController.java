package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.entity.Account;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountRepository accountRepository;

    // ADMIN seulement : voir tous les comptes
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/allaccount")
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    // ADMIN + AGENT_FINANCE + AGENT_ASSURANCE : voir un compte
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_FINANCE','AGENT_ASSURANCE','CLIENT')")
    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id " + id));
    }

    // ADMIN seulement : créer un compte
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/addaccount")
    public Account createAccount(@RequestBody Account account) {
        return accountRepository.save(account);
    }

    // ADMIN seulement : modifier un compte
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Account updateAccount(@PathVariable Long id, @RequestBody Account accountDetails) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id " + id));

        account.setBalance(accountDetails.getBalance());
        account.setType(accountDetails.getType());
        account.setStatus(accountDetails.getStatus());
        account.setClient(accountDetails.getClient());

        return accountRepository.save(account);
    }

    // ADMIN seulement : supprimer un compte
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteAccount(@PathVariable Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id " + id));

        accountRepository.delete(account);
        return "Account deleted successfully!";
    }

    // ADMIN seulement : définir les limites
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/limits")
    public Account setLimits(@PathVariable Long id,
                             @RequestParam double dailyLimit,
                             @RequestParam double monthlyLimit) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id " + id));

        account.setDailyLimit(dailyLimit);
        account.setMonthlyLimit(monthlyLimit);

        return accountRepository.save(account);
    }
}