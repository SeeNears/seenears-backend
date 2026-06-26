package com.seenears.users.controller;

import com.seenears.global.response.ApiResponse;
import com.seenears.users.dto.response.UserMeResponse;
import com.seenears.users.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getMe(Authentication authentication) {
        return ApiResponse.success(userService.getMe(authentication.getName()));
    }
}
