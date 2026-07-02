package com.example.BalisongFlipping.dtos;

import com.example.BalisongFlipping.modals.accounts.Account;

public record GoogleSignInResult(Account account, boolean isNewUser) {}
