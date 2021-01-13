package com.orioninc.combplanuserservice.service;

import com.orioninc.combplanuserservice.dto.UserDto;

public interface UserService {
    UserDto getUserById(Long id);
    UserDto createUser(UserDto userDto);
    UserDto updateUser(UserDto userDto);
    void deleteUser(Long id);
}
