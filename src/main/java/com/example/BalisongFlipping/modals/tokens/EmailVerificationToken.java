package com.example.BalisongFlipping.modals.tokens;

import com.example.BalisongFlipping.modals.accounts.Account;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Random;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    @Column(nullable = false)
    private Instant expiration;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private Account owner;

    public EmailVerificationToken() {}

    public EmailVerificationToken(Account u) {
        this.owner = u;
        this.expiration = Instant.now().plusMillis(600000);
        this.token = generateToken();
    }

    public String generateToken() {
        StringBuilder t = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            t.append(random.nextInt(9) + 1);
        }
        return t.toString();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Instant getExpiration() { return expiration; }
    public void setExpiration(Instant expiration) { this.expiration = expiration; }

    public Account getOwner() { return owner; }
    public void setOwner(Account owner) { this.owner = owner; }
}
