package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Service.IAdminService;
import org.example.projet_pi.entity.Admin;
import org.example.projet_pi.entity.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/admins")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;

    //  Ajouter admin (ADMIN seulement)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public Admin addAdmin(@Valid @RequestBody Admin admin) {
        admin.setRole(Role.ADMIN);
        return adminService.addAdmin(admin);
    }

    //  Modifier admin (ADMIN seulement)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    public Admin updateAdmin(@Valid @RequestBody Admin admin) {
        admin.setRole(Role.ADMIN);
        return adminService.updateAdmin(admin);
    }

    //  Supprimer admin (ADMIN seulement)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public void deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
    }

    //  Voir un admin (ADMIN seulement)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public Admin getAdminById(@PathVariable Long id) {
        return adminService.getAdminById(id);
    }

    // Voir tous les admins (ADMIN seulement)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public List<Admin> getAllAdmins() {
        return adminService.getAllAdmins();
    }
}