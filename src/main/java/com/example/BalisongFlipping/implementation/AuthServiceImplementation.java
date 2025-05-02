package com.example.BalisongFlipping.implementation;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.BalisongFlipping.dtos.LoginAccountDto;
import com.example.BalisongFlipping.dtos.RegisterAccountDto;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.collections.Collection;
import com.example.BalisongFlipping.repositories.AccountRepository;
import com.example.BalisongFlipping.repositories.CollectionRepository;
import com.example.BalisongFlipping.services.AccountService;
import com.example.BalisongFlipping.services.AuthService;

@Service
public class AuthServiceImplementation implements AuthService {
    // object to access mongo db repo containing all accounts
    private final AccountRepository accountRepository;
    private final CollectionRepository collectionRepository;

    @Autowired
    private AccountService accountService;

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
    public Account signup(RegisterAccountDto newUser) {
        // check for account containing passed email already existing
        Optional<Account> holder = accountRepository.findAccountByEmail(newUser.email());
        if (holder.isPresent()) {
            return null;
        }

        // create new user in db
        User u = accountRepository.insert(createNewUser(newUser));

        // create new collection for user
        Collection c = collectionRepository.insert(new Collection(u.getId()));

        // update new user with collection id
        u.setCollectionId(c.getId());

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
}
