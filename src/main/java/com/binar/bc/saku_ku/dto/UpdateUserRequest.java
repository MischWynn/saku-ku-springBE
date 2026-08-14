package com.binar.bc.saku_ku.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
        private String namaLengkap;
        private String email;
        private String status;
        private String roleName;
}
