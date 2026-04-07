package org.example.projet_pi.Service;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.entity.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    // Générer OTP
    public String generateOtp(){

        return String.valueOf((int)(Math.random()*900000)+100000);
    }

    // Envoyer OTP
    public void sendOtp(String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateOtp();

        user.setOtp(otp);

        user.setOtpExpiry(
                new Date(System.currentTimeMillis() + 5 * 60 * 1000)
        );

        userRepository.save(user);

        // Email message
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset OTP");
        message.setText("Your OTP is : " + otp);

        mailSender.send(message);
    }

    // Vérifier OTP + changer password
    public void verifyOtpAndReset(
            String email,
            String otp,
            String newPassword){

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        if(!user.getOtp().equals(otp)){
            throw new RuntimeException("Invalid OTP");
        }

        if(user.getOtpExpiry().before(new Date())){
            throw new RuntimeException("OTP expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));

        user.setOtp(null);
        user.setOtpExpiry(null);

        userRepository.save(user);
    }
}