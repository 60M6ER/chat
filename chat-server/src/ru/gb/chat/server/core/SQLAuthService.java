package ru.gb.chat.server.core;

public class SQLAuthService implements AuthService {

    @Override
    public String nicknameByLoginAndPassword(String login, String password) {
        return SqlClient.getNickname(login, password);
    }
}
