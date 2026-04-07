package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class PasswordOtpController {

    private final OtpService otpService;

    // Envoyer OTP
    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@RequestParam String email){

        otpService.sendOtp(email);

        return ResponseEntity.ok("OTP sent to email");
    }

    // Vérifier OTP + reset password
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword){

        otpService.verifyOtpAndReset(email, otp, newPassword);

        return ResponseEntity.ok("Password changed successfully");
    }
}