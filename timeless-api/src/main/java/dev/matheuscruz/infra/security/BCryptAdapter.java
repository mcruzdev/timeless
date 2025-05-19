package dev.matheuscruz.infra.security;

import org.mindrot.jbcrypt.BCrypt;

public interface BCryptAdapter {

    static String encrypt(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    static Boolean checkPassword(String plaintext, String hash) {
        return BCrypt.checkpw(plaintext, hash);
    }
}
