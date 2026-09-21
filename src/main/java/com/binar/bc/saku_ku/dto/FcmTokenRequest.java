package com.binar.bc.saku_ku.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FcmTokenRequest {

    @NotBlank(message = "fcmToken cannot be empty")
    private String fcmToken;
}
