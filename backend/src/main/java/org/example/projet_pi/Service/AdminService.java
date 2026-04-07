package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.AdminRepository;
import org.example.projet_pi.entity.Admin;
import org.example.projet_pi.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService implements IAdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository,
                        PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmail(email);
    }

    @Override
    public Admin addAdmin(Admin admin) {

        admin.setRole(Role.ADMIN);

        if(admin.getPassword() != null && !admin.getPassword().isEmpty()){
            admin.setPassword(
                    passwordEncoder.encode(admin.getPassword())
            );
        }

        return adminRepository.save(admin);
    }
    @Override
    public Admin updateAdmin(Admin admin) {

        Admin existingAdmin = adminRepository.findById(admin.getId())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if(admin.getFirstName()!=null && !admin.getFirstName().isEmpty())
            existingAdmin.setFirstName(admin.getFirstName());

        if(admin.getLastName()!=null && !admin.getLastName().isEmpty())
            existingAdmin.setLastName(admin.getLastName());

        if(admin.getEmail()!=null && !admin.getEmail().isEmpty())
            existingAdmin.setEmail(admin.getEmail());

        if(admin.getTelephone()!=null && !admin.getTelephone().isEmpty())
            existingAdmin.setTelephone(admin.getTelephone());

        // ✅ Password change + recrypt
        if(admin.getPassword()!=null && !admin.getPassword().isEmpty()){
            existingAdmin.setPassword(
                    passwordEncoder.encode(admin.getPassword())
            );
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
}