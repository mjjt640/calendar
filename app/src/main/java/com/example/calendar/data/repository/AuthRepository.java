package com.example.calendar.data.repository;

import com.example.calendar.domain.model.AuthSession;

public interface AuthRepository {
    AuthSession login(String account, String password);

    AuthSession getSession();

    void logout();
}
