package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Service.IAdminService;
import org.example.projet_pi.entity.Admin;
import org.example.projet_pi.entity.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admins")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;
    private final PasswordEncoder passwordEncoder;  // ✅ Add this line

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/add", consumes = "multipart/form-data")
    public Admin addAdmin(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("telephone") String telephone,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) {
        Admin admin = new Admin();
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setEmail(email);
        admin.setPassword(password);
        admin.setTelephone(telephone);
        admin.setRole(Role.ADMIN);

        return adminService.addAdmin(admin, photo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/update/{id}", consumes = "multipart/form-data")
    public Admin updateAdmin(
            @PathVariable Long id,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String telephone,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) {
        Admin admin = new Admin();
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setEmail(email);
        admin.setPassword(password);
        admin.setTelephone(telephone);

        return adminService.updateAdminById(id, admin, photo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public void deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public Admin getAdminById(@PathVariable Long id) {
        return adminService.getAdminById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public List<Admin> getAllAdmins() {
        return adminService.getAllAdmins();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/change-password/{id}")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @RequestParam String oldPassword,
            @RequestParam String newPassword
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== PASSWORD CHANGE REQUEST ===");
            System.out.println("Admin ID: " + id);
            System.out.println("Old Password Length: " + oldPassword.length());
            System.out.println("New Password Length: " + newPassword.length());

            adminService.changePassword(id, oldPassword, newPassword);

            response.put("success", true);
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.err.println("Error changing password: " + e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ✅ Temporary debug endpoint to check stored password
    @GetMapping("/debug-password/{id}")
    public ResponseEntity<?> debugPassword(@PathVariable Long id) {
        try {
            Admin admin = adminService.getAdminById(id);
            Map<String, Object> debug = new HashMap<>();
            debug.put("adminId", admin.getId());
            debug.put("email", admin.getEmail());
            debug.put("storedPasswordHash", admin.getPassword());
            debug.put("passwordEncoderType", passwordEncoder.getClass().getSimpleName());
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // ✅ Temporary endpoint to force reset password
    @PostMapping("/force-reset-password/{id}")
    public ResponseEntity<?> forceResetPassword(
            @PathVariable Long id,
            @RequestParam String newPassword
    ) {
        try {
            Admin admin = adminService.getAdminById(id);
            admin.setPassword(passwordEncoder.encode(newPassword));
            adminService.updateAdminById(id, admin, null);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password has been reset to: " + newPassword);
            response.put("encodedPassword", admin.getPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}