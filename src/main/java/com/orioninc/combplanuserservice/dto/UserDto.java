package com.orioninc.combplanuserservice.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserDto extends AbstractDto {
    private String login;
    private String password;
    private String email;
    private String fullName;
    private Set<Role> roles;
}
