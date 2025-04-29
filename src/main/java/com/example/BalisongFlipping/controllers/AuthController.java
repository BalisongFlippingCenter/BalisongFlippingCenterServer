package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.BalisongFlippingApplication;
import com.example.BalisongFlipping.dtos.*;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.tokens.RefreshToken;
import com.example.BalisongFlipping.services.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;


@RequestMapping("/auth")
@RestController
public class AuthController {
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    Logger log = LoggerFactory.getLogger(BalisongFlippingApplication.class); 

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private AccountService accountService;

    private final AuthService authenticationService;

    public AuthController(JwtService jwtService, RefreshTokenService refreshTokenService, AuthService authenticationService) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/check-token")
    public ResponseEntity<?> checkToken(@RequestParam String token) {

        return ResponseEntity.ok("token valid");
    }

    /**
     *
     * @param registerUserDto
     * @return
     *
     * POST ENDPOINT
     * - Takes in a body of params to register a new user. Check RegisterAccountDto for details
     * - Calls Auth Service to attempt to store new user in DB
     * - todo -- Implement defensive programming that checks incoming body for correct data
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterAccountDto registerUserDto) {
        // validate new user info
        if (!authenticationService.validateNewUser(registerUserDto)) {
            return ResponseEntity.badRequest().body("Invalid user info passed.");
        }

        // create new user in db
        Account registeredUser = authenticationService.signup(registerUserDto);

        // return if email is found to already exist
        if (registeredUser == null) {
            return ResponseEntity.badRequest().body("Email already exists.");
        }

        // successful account creation
        return ResponseEntity.ok(registeredUser.getId() + " successfully created.");
    }

    @GetMapping("refresh-access-token")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request) {
        // get cookies
        Cookie[] cookies = request.getCookies();

        try {
            // search for refresh token
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("Refresh-Token-Cookie")) {
                    // check for empty refresh token
                    if (refreshTokenService.findByToken(cookie.getValue()).isEmpty())
                        return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);

                    // verify refresh token
                    RefreshToken refreshToken = refreshTokenService.verityExpiration(refreshTokenService.findByToken(cookie.getValue()).get());

                    // generate new access token and return
                    return new ResponseEntity<>(jwtService.generateAccessToken(refreshToken.getOwner()), HttpStatus.OK);
                }
            }

            // return unauthorized due to no refresh token found
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Exception caught refresh-access-token GetMapping -> ", e);
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // get cookies
        Cookie[] cookies = request.getCookies();

        try {
            if (cookies != null) {
                // search for refresh token
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("Refresh-Token-Cookie")) {
                        // remove refresh token from repo
                        refreshTokenService.removeRefreshToken(cookie.getValue());
                    }
                    // set empty refresh token
                    response.addCookie(new Cookie("Refresh-Token-Cookie", ""));
                }
            }

            // return success
            return new ResponseEntity<>("Successfully logged out user.", HttpStatus.OK);
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<>("Failed to logout", HttpStatus.CONFLICT);
        }
    }

    /**
     *
     * @param loginUserDto
     * @return
     *
     * POST ENDPOINT
     * - Takes in a body of params to check for an authenticated user.
     * - Translation... attempts to log in a user by checking user credentials and jwt passed
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginAccountDto loginUserDto, HttpServletResponse response) throws Exception {
        // attempts to retrieve account from authentication service
        try {
            Account authenticatedUser = authenticationService.authenticate(loginUserDto);
            User account = (User) authenticatedUser;

            // creates new access token
            String accessToken = jwtService.generateAccessToken(authenticatedUser);

            // create new refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(loginUserDto.email());

            // set refresh token in cookie
            Cookie refreshTokenCookie = new Cookie("Refresh-Token-Cookie", refreshToken.getToken());
            refreshTokenCookie.setHttpOnly(true);

            // add cookie to response body
            response.addCookie(refreshTokenCookie);

            // get collection data
            CollectionDataDto collectionData = collectionService.getCollection(account.getCollectionId());

            // return account info with access token
            return new ResponseEntity<>(new LoginResponseDto(accessToken, AccountService.convertAccountToDto(authenticatedUser), collectionData), HttpStatus.OK);
        }
        catch(Exception e) {
            return new ResponseEntity<>("Failed: " + e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/refresh-token-login")
    public ResponseEntity<?> authenticateWithRefreshToken(HttpServletRequest request) throws Exception {
        try {
            // get cookies from request
            Cookie[] cookies = request.getCookies();

            // check for no cookies
            if (cookies == null || cookies.length == 0) {
                throw new Exception("No Refresh Token");
            }

            // loop to find refresh token
            for (Cookie cookie: cookies) {
                if (cookie.getName().equals("Refresh-Token-Cookie")) {
                    String refreshToken = cookie.getValue();

                    // check for empty refresh token
                    if (refreshToken.isEmpty()) {
                        throw new Exception("No Refresh Token after cookies search");
                    }

                    // validate refresh token
                    RefreshToken verifiedToken = refreshTokenService.verityExpiration(refreshTokenService.findByToken(refreshToken).get());

                    LoginResponseDto loginResponse = new LoginResponseDto(
                            jwtService.generateAccessToken(verifiedToken.getOwner()),
                            AccountService.convertAccountToDto(accountService.getAccount(verifiedToken.getOwner().getId())),
                            collectionService.getCollectionByAccountId(verifiedToken.getOwner().getId())
                    );

                    return new ResponseEntity<>(loginResponse, HttpStatus.OK);
                }
            }

            throw new Exception("Error with authorizing with cookie.");
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
    }
}
