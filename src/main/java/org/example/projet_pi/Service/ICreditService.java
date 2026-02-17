package org.example.projet_pi.Service;


import org.example.projet_pi.entity.Credit;

import java.util.List;

public interface ICreditService {
    //Iservice

    public Credit addCredit(Credit credit);
    public Credit updateCredit(Credit credit);
    public void deleteCredit(Long id);
    public Credit getCreditById(Long id);
    public List<Credit> getAllCredits();

}
