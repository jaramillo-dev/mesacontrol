package com.biometec.mesacontrol.entity;

import org.springframework.security.core.GrantedAuthority;

public enum Rol implements GrantedAuthority {
    ADMIN("ADMIN"),
    VENTAS("VENTAS"),
    TECNICO("TECNICO");

    private final String authority;

    Rol(String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return "ROLE_" + authority;
    }

    public String getValue() {
        return authority;
    }
}