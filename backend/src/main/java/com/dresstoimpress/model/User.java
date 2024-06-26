package com.dresstoimpress.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @NotBlank(message = "First name must not be empty!")
    private String firstName;

    @NotNull
    @NotBlank(message = "Last name must not be empty!")
    private String lastName;

    @Column(unique = true)
    @NotNull
    @NotBlank(message = "The email must not be empty!")
    @Email(message = "Wrong email format.")
    private String email;

    @NotNull
    @NotEmpty(message = "The password must not be empty!")
    @Size(min = 8, message = "The password length must consist of at least 8 characters.")
    private String password;

    @Column(columnDefinition = "boolean default false")
    private boolean emailVerified;

    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE)
    private EmailVerificationToken emailVerificationToken;

    private LocalDate verificationDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ClothesChange> clothesChanges;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<FavoriteImage> favoriteImages;
}