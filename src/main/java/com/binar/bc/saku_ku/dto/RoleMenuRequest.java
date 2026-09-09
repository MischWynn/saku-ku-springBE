package com.binar.bc.saku_ku.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RoleMenuRequest {
    private UUID roleId;
    private UUID menuId;
    private Boolean canView;
    private Boolean canCreate;
    private Boolean canUpdate;
    private Boolean canDelete;
}
