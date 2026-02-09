package org.example.app.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.app.service.AuthService;
import org.example.app.web.model.AuthRequestModel;
import org.example.app.web.model.AuthResponseModel;
import org.example.app.web.model.RegisterRequestModel;
import org.example.app.web.model.RegisterInviteRequestModel;
import org.example.app.web.model.ResponseModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ResponseModel<AuthResponseModel>> register(@RequestBody RegisterRequestModel request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/register/invite")
    public ResponseEntity<ResponseModel<AuthResponseModel>> registerInvite(@RequestBody RegisterInviteRequestModel request) {
        return ResponseEntity.ok(authService.registerInvite(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseModel<AuthResponseModel>> login(@RequestBody AuthRequestModel request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
