package com.example.BalisongFlipping.implementation;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.BalisongFlipping.dtos.GoogleSignInResult;
import com.example.BalisongFlipping.dtos.LoginAccountDto;
import com.example.BalisongFlipping.dtos.RegisterAccountDto;
import com.example.BalisongFlipping.utils.ProfanityFilter;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.collections.Collection;
import com.example.BalisongFlipping.modals.tokens.EmailVerificationToken;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.CollectionRepository;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.AuthService;
import com.example.BalisongFlipping.services.EmailService;

@Service
public class AuthServiceImplementation implements AuthService {
    // object to access mongo db repo containing all accounts
    private final AccountRepository accountRepository;
    private final CollectionRepository collectionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RestTemplate restTemplate;

    // object for password encoder
    private final PasswordEncoder passwordEncoder;

    // object to access and use spring boot authentication manager
    private final AuthenticationManager authenticationManager;

    /**
     *
     * @param userRepository        accounts repository in db
     * @param authenticationManager spring boot auth manager
     * @param passwordEncoder       password encoder and decoder for spring boot
     *                              auth
     *
     *                              Constructor for Service
     */
    public AuthServiceImplementation(
            AccountRepository userRepository,
            CollectionRepository collectionRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.accountRepository = userRepository;
        this.collectionRepository = collectionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /***
     *
     * @param newUser Object containing email and password for new user along with
     *                their display name
     * @return new User object to store in db
     *
     *         Function takes in an object containing registration information for a
     *         new
     *         user and creates a new user
     *         to be stored in the db
     */
    private User createNewUser(RegisterAccountDto newUser) {
        return new User(
                newUser.email(),
                passwordEncoder.encode(newUser.password()),
                newUser.displayName(),
                accountService.generateIdentifierCode(newUser.displayName()));
    }

    private void sendEmailVerification(String userEmail) throws Exception {
        EmailVerificationToken token = emailService.createNewEmailVerificationToken(userEmail); 
        emailService.sendEmail(userEmail, "Email Verification", "Your verification code is\n -> " + token.getToken() + " <-");
    }

    @Override
    public void reSendEmailToken(String email) throws Exception {
        EmailVerificationToken newToken = emailService.createReplacementEmailVerificationToken(email); 
        emailService.sendEmail(email, "Email Verification", "Your verification code is\n -> " + newToken.getToken() + " <-");
    }

    @Override
    public Boolean validateEmailVerification(String emailToken) throws Exception {
        return emailService.validateEmailTokenVerification(emailToken); 
    }   

    /***
     *
     * @param newUser Object container registration info
     * @return true if valid info, false otherwise
     *
     *         Function goes through each values for a new user to register and
     *         validates
     *         each,
     *         password for password length and complexity,
     *         email is in valid email format,
     *         display name is a friendly user display name
     */
    @Override
    public boolean validateNewUser(RegisterAccountDto newUser) {
        // check for password or email being empty
        if (newUser.password().isEmpty() || newUser.email().isEmpty()) {
            return false;
        }

        // checks password length
        if (newUser.password().length() < 7) {
            return false;
        }

        // validate display name: min 4 chars, allowed chars only, no profanity
        String displayName = newUser.displayName();
        if (displayName == null || displayName.length() < 4) return false;
        for (char c : displayName.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '!' && c != '.') return false;
        }
        if (ProfanityFilter.containsProfanity(displayName)) return false;

        // all checks passed
        return true;
    }

    /***
     *
     * @param newUser Object container new registration info
     * @return null if an account is found to already exist with the passed email,
     *         new account otherwise
     *
     *         Function takes in an object for a new registered user and uses that
     *         info to
     *         create a new account in the db
     */
    @Override
    public Account signup(RegisterAccountDto newUser)  throws Exception {
        // check for account containing passed email already existing
        Optional<Account> holder = accountRepository.findAccountByEmail(newUser.email());
        if (holder.isPresent()) {
            return null;
        }

        // create new user in db
        User u = accountRepository.save(createNewUser(newUser));

        // create new collection for user
        Collection c = collectionRepository.save(new Collection(u.getId()));

        // update new user with collection id and auto-verify email (email verification deferred)
        u.setCollectionId(c.getId());
        u.setEmailVerified(true);

        // save and return new user in db
        return accountRepository.save(u);
    }

    /***
     *
     * @param loginInfo object containing login info
     * @return account once validated, null otherwise
     *
     *         Function authenticates a user based on their email and password
     *         passed to
     *         server
     */
    @Override
    public Account authenticate(LoginAccountDto loginInfo) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginInfo.email(),
                        loginInfo.password()));

        return accountRepository.findAccountByEmail(loginInfo.email())
                .orElseThrow();
    }

    @Override
    public GoogleSignInResult googleSignIn(String googleAccessToken) throws Exception {
        // Fetch user info from Google
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(googleAccessToken);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new Exception("Invalid Google access token.");
        }

        Map<String, Object> userInfo = response.getBody();
        String email = (String) userInfo.get("email");
        if (email == null) throw new Exception("Google account has no email address.");

        String givenName = (String) userInfo.get("given_name");
        String fullName  = (String) userInfo.get("name");

        // Existing user — log in
        Optional<Account> existing = accountRepository.findAccountByEmail(email);
        if (existing.isPresent()) {
            return new GoogleSignInResult(existing.get(), false);
        }

        // New user — create account with a temporary display name
        String tempDisplayName = buildTempDisplayName(givenName != null ? givenName : fullName);
        User newUser = new User(
                email,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                tempDisplayName,
                accountService.generateIdentifierCode(tempDisplayName)
        );
        newUser.setEmailVerified(true);
        User saved = accountRepository.save(newUser);

        Collection collection = collectionRepository.save(new Collection(saved.getId()));
        saved.setCollectionId(collection.getId());

        return new GoogleSignInResult(accountRepository.save(saved), true);
    }

    private String buildTempDisplayName(String googleName) {
        if (googleName != null && !googleName.isBlank()) {
            String sanitized = googleName.replaceAll("[^a-zA-Z0-9_!.]", "");
            if (sanitized.length() >= 4) {
                return sanitized.length() > 20 ? sanitized.substring(0, 20) : sanitized;
            }
        }
        return "user" + (100000 + new Random().nextInt(900000));
    }
}
