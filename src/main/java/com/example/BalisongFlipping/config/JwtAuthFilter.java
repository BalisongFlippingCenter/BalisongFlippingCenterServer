package com.example.BalisongFlipping.config;

import com.example.BalisongFlipping.modals.tokens.RefreshToken;
import com.example.BalisongFlipping.services.JwtService;
import com.example.BalisongFlipping.services.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final HandlerExceptionResolver handlerExceptionResolver;

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserDetailsService userDetailsService,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // get access token from auth
            String accessToken = getAccessToken(request);

            // check if auth failed
            if (accessToken.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            // get user email with access token
            final String userEmail = jwtService.extractUsername(accessToken);

            // get auth object
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // if auth object matches user email in user details
            if (userEmail != null && authentication == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // check if access token is valid
                if (jwtService.isAccessTokenValid(accessToken, userDetails)) {
                    System.out.println("Token is valid.");
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
                else {
                    // check for refresh token if access token is invalid
                    Cookie[] cookies = request.getCookies();

                    try {
                        for (Cookie cookie: cookies) {
                            if (cookie.getName().equals("Refresh-Token-Cookie")) {
                                if (!refreshTokenService.findByToken(cookie.getValue()).isEmpty()) {
                                    RefreshToken refreshToken = refreshTokenService.verityExpiration(refreshTokenService.findByToken(cookie.getValue()).get());
                                    jwtService.generateAccessToken(refreshToken.getOwner()); 
                                    break;
                                }
                            }
                            else {
                                return;
                            }
                        }
                    }
                    catch (Exception e) {
                        throw e;
                    }
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            // set response status to 403 for unauthorized
            response.setStatus(403);
            response.getWriter().println(exception.getMessage());
            // handle exception
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }

    private String getAccessToken(HttpServletRequest request) {
        // check for empty or invalid header

        if (request.getHeader("Authorization") == null)
            return "";

        if (!request.getHeader("Authorization").startsWith("Bearer "))
            return "";

        return request.getHeader("Authorization").substring(7);
    }
}
