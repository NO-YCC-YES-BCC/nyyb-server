package com.nyyb.nyybserver.common.security;

import com.nyyb.nyybserver.user.data.enums.UserRole;

import java.security.Principal;

public record UserPrincipal(Long id, UserRole role) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
