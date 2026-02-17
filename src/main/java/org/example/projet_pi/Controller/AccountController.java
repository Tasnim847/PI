package org.example.projet_pi.Controller;

import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.entity.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    //crud
    @Autowired
    private AccountRepository accountRepository;

    // 🔹 Get all accounts
    @GetMapping
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    // 🔹 Get account by id
    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id " + id));
    }

    // 🔹 Create new account
    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountRepository.save(account);
    }

    // 🔹 Update account
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

    // 🔹 Delete account
    @DeleteMapping("/{id}")
    public String deleteAccount(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id " + id));

        accountRepository.delete(account);
        return "Account deleted successfully!";
    }
}
