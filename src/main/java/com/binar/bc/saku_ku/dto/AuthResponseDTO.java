package com.binar.bc.saku_ku.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    
    private String token;
    private String type="Bearer";

    public AuthResponseDTO(String token) {
        this.token = token;
    }

}
