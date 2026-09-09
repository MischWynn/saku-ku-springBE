// DTO baru: dto/UserProfileDTO.java
package com.binar.bc.saku_ku.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileDTO {
    private String namaLengkap;
    private String roleName; // nama_role, misal "SUPERADMIN" / "MARKETING"
}