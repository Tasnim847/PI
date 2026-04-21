
package org.example.projet_pi.Service;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.AdminRepository;
import org.example.projet_pi.entity.Admin;
import org.example.projet_pi.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService implements IAdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmail(email);
    }

    @Override
    public Admin addAdmin(Admin admin, MultipartFile photo) {

        admin.setRole(Role.ADMIN);

        if(admin.getPassword() != null && !admin.getPassword().isEmpty()){
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }

        // Upload photo
        if(photo != null && !photo.isEmpty()){
            String fileName = uploadPhoto(photo);
            admin.setPhoto(fileName);
        }

        return adminRepository.save(admin);
    }

    @Override
    public Admin updateAdminById(Long id, Admin admin, MultipartFile photo) {

        Admin existingAdmin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if(admin.getFirstName() != null && !admin.getFirstName().isEmpty())
            existingAdmin.setFirstName(admin.getFirstName());

        if(admin.getLastName() != null && !admin.getLastName().isEmpty())
            existingAdmin.setLastName(admin.getLastName());

        if(admin.getEmail() != null && !admin.getEmail().isEmpty())
            existingAdmin.setEmail(admin.getEmail());

        if(admin.getTelephone() != null && !admin.getTelephone().isEmpty())
            existingAdmin.setTelephone(admin.getTelephone());

        if(admin.getPassword() != null && !admin.getPassword().isEmpty())
            existingAdmin.setPassword(passwordEncoder.encode(admin.getPassword()));

        // Update photo
        if(photo != null && !photo.isEmpty()){
            String fileName = uploadPhoto(photo);
            existingAdmin.setPhoto(fileName);
        }

        existingAdmin.setRole(Role.ADMIN);

        return adminRepository.save(existingAdmin);
    }

    @Override
    public void deleteAdmin(Long id) {
        adminRepository.deleteById(id);
    }

    @Override
    public Admin getAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    private String uploadPhoto(MultipartFile file) {
        try {
            String uploadDir = "uploads/";
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            Path path = Paths.get(uploadDir + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Erreur upload photo");
        }
    }
    @Override
    public void changePassword(Long adminId, String oldPassword, String newPassword) {
        System.out.println("=== CHANGE PASSWORD SERVICE ===");
        System.out.println("Admin ID: " + adminId);
        System.out.println("Old password provided: " + oldPassword);
        System.out.println("New password: " + newPassword);

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminId));

        System.out.println("Found admin: " + admin.getEmail());
        System.out.println("Stored password hash: " + admin.getPassword());
        System.out.println("Password encoder matches: " + passwordEncoder.matches(oldPassword, admin.getPassword()));

        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            System.err.println("Password mismatch for admin: " + adminId);
            throw new RuntimeException("Current password is incorrect");
        }

        String encodedNewPassword = passwordEncoder.encode(newPassword);
        System.out.println("New encoded password: " + encodedNewPassword);

        admin.setPassword(encodedNewPassword);
        adminRepository.save(admin);

        System.out.println("Password changed successfully for admin: " + adminId);
    }

}