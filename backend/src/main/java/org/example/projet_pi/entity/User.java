package org.example.projet_pi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only letters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Last name must contain only letters")
    private String lastName;
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private FaceData faceData;
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid (example: exemple@exemple.com)")
    @Column(unique = true)
    private String email;

    @Pattern(
            regexp = "^$|^(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$",
            message = "Password must be at least 8 characters, contain 1 uppercase letter and 1 symbol"
    )
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // ✅ Nullable : pas obligatoire pour les utilisateurs OAuth2
    @Column(nullable = true)
    @Pattern(
            regexp = "^$|^\\+216\\d{8}$",
            message = "Telephone must start with +216 and contain 8 digits"
    )
    private String telephone;

    private String otp;
    private Date otpExpiry;
    private Integer loginAttempts = 0;
    private Boolean accountNonLocked = true;
    private Date lockTime;
    private String photo;

    @Enumerated(EnumType.STRING)
    private Role role;

    // ✅ Nouveau flag pour distinguer les comptes OAuth2
    @Column(nullable = true)
    private Boolean oauthUser = false;

    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(this.accountNonLocked);
    }
}