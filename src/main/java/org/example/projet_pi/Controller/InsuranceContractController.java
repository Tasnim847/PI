package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.IInsuranceContractService;
import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.example.projet_pi.Mapper.InsuranceContractMapper;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Service.PdfGenerationService;
import org.example.projet_pi.entity.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/contrats")
public class InsuranceContractController {

    private final IInsuranceContractService contractService;
    private final InsuranceContractRepository contractRepository; // Ajouté
    private final PdfGenerationService pdfGenerationService;

    @PostMapping("/addCont")
    public InsuranceContractDTO addContract(@RequestBody InsuranceContractDTO dto) {
        return contractService.addContract(dto);
    }

    @PutMapping("/updateCont")
    public InsuranceContractDTO updateContract(@RequestBody InsuranceContractDTO dto) {
        return contractService.updateContract(dto);
    }

    @DeleteMapping("/deleteCont/{id}")
    public void deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
    }

    @GetMapping("/getCont/{id}")
    public InsuranceContractDTO getContractById(@PathVariable Long id) {
        return contractService.getContractById(id);
    }

    @GetMapping("/allCont")
    public List<InsuranceContractDTO> getAllContracts() {
        return contractService.getAllContracts();
    }

    // 🔥 NOUVEAU: Activer un contrat
    @PutMapping("/activate/{id}")
    public InsuranceContractDTO activateContract(@PathVariable Long id) {
        InsuranceContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        if (contract.getStatus() == ContractStatus.INACTIVE) {
            contract.setStatus(ContractStatus.ACTIVE);
            contract = contractRepository.save(contract);
        } else {
            throw new RuntimeException("Seuls les contrats INACTIVE peuvent être activés");
        }

        return InsuranceContractMapper.toDTO(contract);
    }

    // 🔥 NOUVEAU: Vérifier tous les contrats
    @PostMapping("/check-late-payments")
    public String checkAllLatePayments() {
        contractService.checkLatePayments();
        return "Vérification des retards effectuée pour tous les contrats";
    }

    // 🔥 NOUVEAU: Vérifier un contrat spécifique
    @PostMapping("/check-late-payments/{id}")
    public String checkContractLatePayments(@PathVariable Long id) {
        contractService.checkContractLatePayments(id);
        return "Vérification du contrat " + id + " effectuée";
    }

    // 🔥 NOUVEAU: Simuler des retards (pour tests)
    @PostMapping("/simulate-late-payments/{id}/{months}")
    public String simulateLatePayments(@PathVariable Long id, @PathVariable int months) {
        contractService.simulateLatePayments(id, months);
        return months + " mois de retard simulés pour le contrat " + id;
    }

    @PostMapping("/check-completed")
    public String checkCompletedContracts() {
        contractService.checkCompletedContracts();
        return "Vérification des contrats COMPLETED effectuée";
    }

    // 🔥 NOUVEAU: Vérifier la fin de mois manuellement
    @PostMapping("/check-end-of-month")
    public String checkEndOfMonth() {
        contractService.checkEndOfMonthLatePayments();
        return "Vérification de fin de mois effectuée";
    }


    /**
     * Télécharger le contrat au format PDF complet
     * URL: GET /contrats/{id}/download/pdf
     */
    @GetMapping("/{id}/download/pdf")
    public ResponseEntity<InputStreamResource> downloadContractPdf(@PathVariable Long id) {
        try {
            // 1. Récupérer le contrat avec toutes ses relations
            InsuranceContract contract = contractRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé avec l'id: " + id));

            // 2. Récupérer les entités associées
            Client client = contract.getClient();
            AgentAssurance agent = contract.getAgentAssurance();
            List<Payment> payments = contract.getPayments();

            if (client == null) {
                throw new RuntimeException("Le contrat n'est pas associé à un client");
            }

            // 3. Générer le PDF
            byte[] pdfContent = pdfGenerationService.generateContractPdf(
                    contract, client, agent, payments
            );

            // 4. Préparer le nom du fichier
            String filename = String.format("contrat_%d_%s.pdf",
                    id,
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
            );

            // 5. Retourner le PDF en pièce jointe
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfContent.length)
                    .body(new InputStreamResource(new ByteArrayInputStream(pdfContent)));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

}