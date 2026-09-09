package com.binar.bc.saku_ku.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MenuRequest {
    private String namaMenu;
    private String path;
    private String icon;
    private UUID parentId;
    private Integer urutan;
    private String status;
}
