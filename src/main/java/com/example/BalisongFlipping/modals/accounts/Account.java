package com.example.BalisongFlipping.modals.accounts;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Entity
@Table(name = "accounts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("ACCOUNT")
public class Account implements UserDetails {

    public Account() {}

    public Account(String email, String password) {
        this.email = email;
        this.emailVerified = false;
        this.password = password;
        this.accountCreationDate = new Date();
        this.lastLogin = new Date();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String role;
    private String password;
    private Date accountCreationDate;
    private Date lastLogin;
    private Boolean emailVerified;

    private boolean banned;
    @Column(columnDefinition = "TEXT")
    private String banReason;

    private Instant suspendedUntil;
    @Column(columnDefinition = "TEXT")
    private String suspendReason;

    private Instant mutedUntil;
    @Column(columnDefinition = "TEXT")
    private String muteReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public void setPassword(String password) { this.password = password; }

    public Date getAccountCreationDate() { return accountCreationDate; }
    public void setAccountCreationDate(Date accountCreationDate) { this.accountCreationDate = accountCreationDate; }

    public Date getLastLogin() { return lastLogin; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }

    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }

    public Instant getSuspendedUntil() { return suspendedUntil; }
    public void setSuspendedUntil(Instant suspendedUntil) { this.suspendedUntil = suspendedUntil; }

    public String getSuspendReason() { return suspendReason; }
    public void setSuspendReason(String suspendReason) { this.suspendReason = suspendReason; }

    public Instant getMutedUntil() { return mutedUntil; }
    public void setMutedUntil(Instant mutedUntil) { this.mutedUntil = mutedUntil; }

    public String getMuteReason() { return muteReason; }
    public void setMuteReason(String muteReason) { this.muteReason = muteReason; }

    public boolean isCurrentlySuspended() { return suspendedUntil != null && suspendedUntil.isAfter(Instant.now()); }
    public boolean isCurrentlyMuted() { return mutedUntil != null && mutedUntil.isAfter(Instant.now()); }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) return List.of();
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return !isCurrentlySuspended(); }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return !banned; }
}
