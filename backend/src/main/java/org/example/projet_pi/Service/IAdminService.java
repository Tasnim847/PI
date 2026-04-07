package org.example.projet_pi.Service;

import org.example.projet_pi.entity.Admin;

import java.util.List;

public interface IAdminService {

    Admin addAdmin(Admin admin);

    Admin updateAdmin(Admin admin);

    void deleteAdmin(Long id);

    Admin getAdminById(Long id);

    List<Admin> getAllAdmins();
}