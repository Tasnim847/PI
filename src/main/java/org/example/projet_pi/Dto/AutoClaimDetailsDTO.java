package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
public class AutoClaimDetailsDTO {

    private Long id;

    private String driverA;
    private String driverB;

    private String vehicleA;
    private String vehicleB;

    private String accidentLocation;
    private Date accidentDate;
}