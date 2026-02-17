package org.example.projet_pi.Service;

import org.example.projet_pi.entity.Compensation;

import java.util.List;

public interface ICompensationService {

    //Iservice

    public Compensation addCompensation(Compensation compensation);

    public Compensation updateCompensation(Compensation compensation);

    public void deleteCompensation(Long id);

    public Compensation getCompensationById(Long id);

    public List<Compensation> getAllCompensations();

}
