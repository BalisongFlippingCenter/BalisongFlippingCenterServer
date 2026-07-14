package com.example.BalisongFlipping.controllers;

import com.example.BalisongFlipping.BalisongFlippingApplication;
import com.example.BalisongFlipping.dtos.*;
import com.example.BalisongFlipping.dtos.ConfirmForgotPasswordDto;
import com.example.BalisongFlipping.dtos.ForgotPasswordDto;
import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import com.example.BalisongFlipping.modals.tokens.RefreshToken;
import com.example.BalisongFlipping.services.*;
import org.springframework.web.client.HttpClientErrorException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import java.time.Duration;

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

    @PostMapping("/resend-email-token/{email}")
    public ResponseEntity<?> resendEmailToken(@PathVariable String email) throws Exception {
        System.out.println("Email: " + email); 
        authenticationService.reSendEmailToken(email);
        return new ResponseEntity<>("Success", HttpStatus.valueOf(200)); 
    }

    @GetMapping("/verify-email-token/{token}")
    public ResponseEntity<?> verifyEmailToken(@PathVariable String token) throws Exception {

        log.trace("Accessing verification end point with token: " + token);
        
        if (authenticationService.validateEmailVerification(token)) {
            return new ResponseEntity<>("Success", HttpStatus.ACCEPTED);
        }
        else {
            return new ResponseEntity<>("Verification Failed", HttpStatus.UNAUTHORIZED);
        }
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
        try {
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
        catch (Exception e) {
            log.info(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT); 
        }
    }

    @GetMapping("refresh-access-token")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request) {
        // get cookies
        Cookie[] cookies = request.getCookies();

        try {
            if (cookies == null) return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);

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
            log.error("Exception caught /refresh-access-token GetMapping -> ", e.getMessage());
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // get cookies
        Cookie[] cookies = request.getCookies();

        try {
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("Refresh-Token-Cookie")) {
                        try {
                            refreshTokenService.removeRefreshToken(cookie.getValue());
                        } catch (Exception ignored) {
                            // token not found or already removed — still expire the cookie
                        }
                        break;
                    }
                }
            }

            // always expire the cookie regardless of whether a token was found
            ResponseCookie expiredCookie = ResponseCookie.from("Refresh-Token-Cookie", "")
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());

            return new ResponseEntity<>("Successfully logged out user.", HttpStatus.OK);
        }
        catch(Exception e) {
            log.error("Exception caught /logout PostMapping -> ", e.getMessage());
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
            ResponseCookie refreshTokenCookie = ResponseCookie.from("Refresh-Token-Cookie", refreshToken.getToken())
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            // get collection data
            CollectionDataDto collectionData = collectionService.getCollection(account.getCollectionId() != null ? account.getCollectionId().toString() : null);

            // return account info with access token
            return new ResponseEntity<>(new LoginResponseDto(accessToken, refreshToken.getToken(), accountService.toUserDto(authenticatedUser), collectionData), HttpStatus.OK);
        }
        catch(Exception e) {
            log.error("Exception caught /login PostMapping -> ", e.getMessage());
            return new ResponseEntity<>("Failed: " + e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleSignIn(@RequestBody GoogleAuthDto dto, HttpServletResponse response) {
        try {
            GoogleSignInResult result = authenticationService.googleSignIn(dto.googleAccessToken());
            Account account = result.account();
            User user = (User) account;

            String accessToken = jwtService.generateAccessToken(account);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(account.getEmail());

            ResponseCookie refreshTokenCookie = ResponseCookie.from("Refresh-Token-Cookie", refreshToken.getToken())
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            CollectionDataDto collectionData = collectionService.getCollection(
                    user.getCollectionId() != null ? user.getCollectionId().toString() : null);

            return new ResponseEntity<>(new GoogleLoginResponseDto(
                    accessToken,
                    refreshToken.getToken(),
                    accountService.toUserDto(account),
                    collectionData,
                    result.isNewUser()
            ), HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            log.error("POST /auth/google -> Google rejected token: {}", e.getMessage());
            return new ResponseEntity<>("Invalid Google access token.", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("POST /auth/google -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordDto dto) {
        try {
            authenticationService.forgotPassword(dto.email());
            return ResponseEntity.ok("If an account exists with that email, a reset code has been sent.");
        } catch (Exception e) {
            log.error("POST /auth/forgot-password -> {}", e.getMessage());
            return ResponseEntity.ok("If an account exists with that email, a reset code has been sent.");
        }
    }

    @PostMapping("/confirm-forgot-password")
    public ResponseEntity<?> confirmForgotPassword(@RequestBody ConfirmForgotPasswordDto dto) {
        try {
            authenticationService.confirmForgotPassword(dto);
            return ResponseEntity.ok("Password updated successfully.");
        } catch (Exception e) {
            log.error("POST /auth/confirm-forgot-password -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PatchMapping("/display-name")
    public ResponseEntity<?> setDisplayName(@RequestBody SetDisplayNameDto dto) {
        try {
            String accountId = accountService.getSelf().id();
            UserDto updated = accountService.setInitialDisplayName(accountId, dto.displayName());
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (Exception e) {
            log.error("PATCH /auth/display-name -> {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/refresh-token-login")
    public ResponseEntity<?> authenticateWithRefreshToken(HttpServletRequest request, @RequestBody(required = false) String tokenFromBody) throws Exception {
        try {
            String refreshTokenValue = null;

            // try cookie first
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("Refresh-Token-Cookie") && !cookie.getValue().isEmpty()) {
                        refreshTokenValue = cookie.getValue();
                        break;
                    }
                }
            }

            // fall back to body (used when cross-origin cookie can't be sent)
            if (refreshTokenValue == null && tokenFromBody != null && !tokenFromBody.isBlank()) {
                refreshTokenValue = tokenFromBody.trim();
            }

            if (refreshTokenValue == null) {
                throw new Exception("No Refresh Token");
            }

            RefreshToken verifiedToken = refreshTokenService.verityExpiration(refreshTokenService.findByToken(refreshTokenValue).get());

            LoginResponseDto loginResponse = new LoginResponseDto(
                    jwtService.generateAccessToken(verifiedToken.getOwner()),
                    verifiedToken.getToken(),
                    accountService.toUserDto(accountService.getAccount(verifiedToken.getOwner().getId().toString())),
                    collectionService.getCollectionByAccountId(verifiedToken.getOwner().getId().toString())
            );

            return new ResponseEntity<>(loginResponse, HttpStatus.OK);
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
    }
}
