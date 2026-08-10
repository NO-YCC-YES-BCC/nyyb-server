package com.nyyb.nyybserver.common.security;

import com.nyyb.nyybserver.user.data.enums.UserRole;

public record UserPrincipal(Long id, UserRole role) {

}
