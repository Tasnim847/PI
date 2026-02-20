package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.CompensationDTO;

import java.util.List;

public interface ICompensationService {

    CompensationDTO addCompensation(CompensationDTO dto);

    CompensationDTO updateCompensation(CompensationDTO dto);

    void deleteCompensation(Long id);

    CompensationDTO getCompensationById(Long id);

    List<CompensationDTO> getAllCompensations();
}