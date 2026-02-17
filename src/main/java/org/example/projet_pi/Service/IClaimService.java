package org.example.projet_pi.Service;

import org.example.projet_pi.entity.Claim;

import java.util.List;

public interface IClaimService {
//Iservice

    public Claim addClaim(Claim claim);

    public Claim updateClaim(Claim claim);

    public void deleteClaim(Long id);

    public Claim getClaimById(Long id);

    public List<Claim> getAllClaims();

}
