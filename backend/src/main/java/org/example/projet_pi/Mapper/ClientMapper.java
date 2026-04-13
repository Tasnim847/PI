package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.ClientDTO;
import org.example.projet_pi.entity.Client;

public class ClientMapper {

    public static ClientDTO toDTO(Client client) {
        if (client == null) return null;

        ClientDTO dto = new ClientDTO();

        // héritage depuis User
        dto.setId(client.getId());
        dto.setFirstName(client.getFirstName());
        dto.setLastName(client.getLastName());
        dto.setEmail(client.getEmail());
        dto.setTelephone(client.getTelephone());

        return dto;
    }
}