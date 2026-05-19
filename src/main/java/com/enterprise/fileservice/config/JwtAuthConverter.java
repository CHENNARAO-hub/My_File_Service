package com.enterprise.fileservice.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;

public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");

            roles.forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role))
            );
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }
}