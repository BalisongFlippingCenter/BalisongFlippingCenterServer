package com.example.BalisongFlipping.services;

import com.example.BalisongFlipping.dtos.LoginAccountDto;
import com.example.BalisongFlipping.dtos.RegisterAccountDto;
import com.example.BalisongFlipping.modals.accounts.Account;

public interface AuthService {

    boolean validateNewUser(RegisterAccountDto newUser);

    Account signup(RegisterAccountDto newUser);
    Account authenticate(LoginAccountDto loginInfo);
}
