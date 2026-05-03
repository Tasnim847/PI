package org.example.projet_pi.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Dto.ClientWithAgentsDTO;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService implements IClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Client addClient(Client client, MultipartFile photo) {
        client.setRole(Role.CLIENT);

        if (client.getPassword() != null && !client.getPassword().isEmpty()) {
            client.setPassword(passwordEncoder.encode(client.getPassword()));
        }

        if (photo != null && !photo.isEmpty()) {
            String fileName = uploadPhoto(photo);
            client.setPhoto(fileName);
        }

        return clientRepository.save(client);
    }

    @Override
    public Client updateClientById(Long id, Client clientRequest, MultipartFile photo) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (clientRequest.getFirstName() != null)
            existingClient.setFirstName(clientRequest.getFirstName());
        if (clientRequest.getLastName() != null)
            existingClient.setLastName(clientRequest.getLastName());
        if (clientRequest.getEmail() != null)
            existingClient.setEmail(clientRequest.getEmail());
        if (clientRequest.getTelephone() != null)
            existingClient.setTelephone(clientRequest.getTelephone());

        // Gestion de la photo
        if (photo != null && !photo.isEmpty()) {
            // Supprimer l'ancienne photo si elle existe
            if (existingClient.getPhoto() != null && !existingClient.getPhoto().isEmpty()) {
                try {
                    String oldPhotoPath = System.getProperty("user.dir") + "/uploads/" + existingClient.getPhoto();
                    Files.deleteIfExists(Paths.get(oldPhotoPath));
                } catch (Exception e) {
                    System.err.println("Could not delete old photo: " + e.getMessage());
                }
            }

            String fileName = uploadPhoto(photo);
            existingClient.setPhoto(fileName);
        }

        return clientRepository.save(existingClient);
    }

    @Override
    @Transactional
    public void deleteClient(Long id) {
        // Vérifier d'abord si le client existe
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));

        try {
            // Désactiver temporairement les vérifications des clés étrangères
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

            // 1. Récupérer tous les IDs des claims du client
            List<Long> claimIds = entityManager.createNativeQuery("SELECT claim_id FROM claim WHERE client_id = ?")
                    .setParameter(1, id)
                    .getResultList();

            // 2. Supprimer les détails des sinistres pour chaque claim
            if (!claimIds.isEmpty()) {
                for (Long claimId : claimIds) {
                    entityManager.createNativeQuery("DELETE FROM auto_claim_details WHERE claim_claim_id = ?")
                            .setParameter(1, claimId)
                            .executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM health_claim_details WHERE claim_claim_id = ?")
                            .setParameter(1, claimId)
                            .executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM home_claim_details WHERE claim_claim_id = ?")
                            .setParameter(1, claimId)
                            .executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM compensation WHERE claim_id = ?")
                            .setParameter(1, claimId)
                            .executeUpdate();
                }
            }

            // 3. Mettre à jour les documents pour enlever la référence aux claims
            if (!claimIds.isEmpty()) {
                for (Long claimId : claimIds) {
                    entityManager.createNativeQuery("UPDATE document SET claim_claim_id = NULL WHERE claim_claim_id = ?")
                            .setParameter(1, claimId)
                            .executeUpdate();
                }
            }

            // 4. Supprimer les documents du client
            entityManager.createNativeQuery("DELETE FROM document WHERE client_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 5. Supprimer les claims
            entityManager.createNativeQuery("DELETE FROM claim WHERE client_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 6. Supprimer les réclamations
            entityManager.createNativeQuery("DELETE FROM complaint WHERE client_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 7. Récupérer tous les IDs des crédits du client
            List<Long> creditIds = entityManager.createNativeQuery("SELECT credit_id FROM credit WHERE client_id = ?")
                    .setParameter(1, id)
                    .getResultList();

            // 8. Supprimer les repayments pour chaque crédit
            if (!creditIds.isEmpty()) {
                for (Long creditId : creditIds) {
                    entityManager.createNativeQuery("DELETE FROM repayment WHERE credit_id = ?")
                            .setParameter(1, creditId)
                            .executeUpdate();
                }
            }

            // 9. Récupérer tous les IDs des contrats du client
            List<Long> contractIds = entityManager.createNativeQuery("SELECT contract_id FROM insurance_contract WHERE client_id = ?")
                    .setParameter(1, id)
                    .getResultList();

            // 10. Supprimer les payments et risk_claims pour chaque contrat
            if (!contractIds.isEmpty()) {
                for (Long contractId : contractIds) {
                    // Pour Payment - la colonne s'appelle contract_contract_id
                    entityManager.createNativeQuery("DELETE FROM payment WHERE contract_contract_id = ?")
                            .setParameter(1, contractId)
                            .executeUpdate();

                    // Pour RiskClaim - la colonne s'appelle contract_id
                    entityManager.createNativeQuery("DELETE FROM risk_claim WHERE contract_id = ?")
                            .setParameter(1, contractId)
                            .executeUpdate();
                }
            }

            // 11. Supprimer les contrats
            entityManager.createNativeQuery("DELETE FROM insurance_contract WHERE client_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 12. Supprimer les crédits
            entityManager.createNativeQuery("DELETE FROM credit WHERE client_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 13. Récupérer tous les IDs des comptes du client
            List<Long> accountIds = entityManager.createNativeQuery("SELECT account_id FROM account WHERE client_id = ?")
                    .setParameter(1, id)
                    .getResultList();

            // 14. Supprimer les transactions pour chaque compte - Utiliser account_id (pas account_account_id)
            if (!accountIds.isEmpty()) {
                for (Long accountId : accountIds) {
                    entityManager.createNativeQuery("DELETE FROM transaction WHERE account_id = ?")
                            .setParameter(1, accountId)
                            .executeUpdate();
                }
            }

            // 15. Supprimer les comptes
            entityManager.createNativeQuery("DELETE FROM account WHERE client_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 16. Supprimer les demandes de compte
            entityManager.createNativeQuery("DELETE FROM account_requests WHERE client_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 17. Supprimer les demandes de paiement cash
            entityManager.createNativeQuery("DELETE FROM cash_payment_requests WHERE client_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 18. Supprimer les rappels de paiement
            entityManager.createNativeQuery("DELETE FROM payment_reminder WHERE payment_id IN (SELECT payment_id FROM payment WHERE contract_contract_id IN (SELECT contract_id FROM insurance_contract WHERE client_id = ?))")
                    .setParameter(1, id)
                    .executeUpdate();

            // 19. Supprimer face_data
            entityManager.createNativeQuery("DELETE FROM face_data WHERE user_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 20. Supprimer login_history
            entityManager.createNativeQuery("DELETE FROM login_history WHERE user_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 21. Supprimer le client
            entityManager.createNativeQuery("DELETE FROM client WHERE id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // 22. Supprimer l'utilisateur
            entityManager.createNativeQuery("DELETE FROM user WHERE id = ?")
                    .setParameter(1, id)
                    .executeUpdate();

            // Réactiver les vérifications des clés étrangères
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

            System.out.println("Client and all associated data deleted successfully for id: " + id);

        } catch (Exception e) {
            // S'assurer que les contraintes sont réactivées même en cas d'erreur
            try {
                entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            } catch (Exception ex) {
                System.err.println("Failed to re-enable foreign key checks: " + ex.getMessage());
            }
            throw new RuntimeException("Error deleting client with id " + id + ": " + e.getMessage(), e);
        }
    }


    @Override
    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
    }

    @Override
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    @Override
    public List<Client> getClientsByAgentFinance(Long agentFinanceId) {
        return clientRepository.findByAgentFinanceId(agentFinanceId);
    }

    @Override
    public List<Client> getClientsByAgentAssurance(Long agentAssuranceId) {
        return clientRepository.findByAgentAssuranceId(agentAssuranceId);
    }

    @Override
    public void changePassword(Long clientId, String oldPassword, String newPassword) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!passwordEncoder.matches(oldPassword, client.getPassword())) {
            throw new RuntimeException("Old password incorrect");
        }

        client.setPassword(passwordEncoder.encode(newPassword));
        clientRepository.save(client);
    }


    private String uploadPhoto(MultipartFile file) {
        try {
            // Chemin absolu vers le dossier uploads dans le projet
            String projectPath = System.getProperty("user.dir");
            String uploadDir = projectPath + "/uploads/";

            // Créer un nom de fichier unique
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            // Créer le dossier s'il n'existe pas
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            // Sauvegarder le fichier
            Path filePath = path.resolve(fileName);
            Files.write(filePath, file.getBytes());

            System.out.println("Photo saved to: " + filePath.toString());

            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur upload photo: " + e.getMessage());
        }
    }

    public List<ClientWithAgentsDTO> getAllClientsWithAgents() {
        List<Client> clients = clientRepository.findAll();

        return clients.stream().map(client -> {
            ClientWithAgentsDTO dto = new ClientWithAgentsDTO();
            dto.setId(client.getId());
            dto.setFirstName(client.getFirstName());
            dto.setLastName(client.getLastName());
            dto.setEmail(client.getEmail());
            dto.setTelephone(client.getTelephone());
            dto.setRole(client.getRole().name());
            dto.setPhoto(client.getPhoto());
            dto.setAnnualIncome(client.getAnnualIncome());
            dto.setCreatedAt(client.getCreatedAt());

            // 🔥 Ajouter l'agent finance (ID et nom)
            if (client.getAgentFinance() != null) {
                dto.setAgentFinanceId(client.getAgentFinance().getId());
                dto.setAgentFinanceName(client.getAgentFinance().getFirstName() + " " + client.getAgentFinance().getLastName());
            } else {
                dto.setAgentFinanceId(null);
                dto.setAgentFinanceName("Not assigned");
            }

            // 🔥 Ajouter l'agent assurance
            if (client.getAgentAssurance() != null) {
                dto.setAgentAssuranceId(client.getAgentAssurance().getId());
                dto.setAgentAssuranceName(client.getAgentAssurance().getFirstName() + " " + client.getAgentAssurance().getLastName());
            } else {
                dto.setAgentAssuranceId(null);
                dto.setAgentAssuranceName("Not assigned");
            }

            return dto;
        }).collect(Collectors.toList());
    }
}