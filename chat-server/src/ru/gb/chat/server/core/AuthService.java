package ru.gb.chat.server.core;

public interface AuthService {

    String nicknameByLoginAndPassword(String login, String password);
}
