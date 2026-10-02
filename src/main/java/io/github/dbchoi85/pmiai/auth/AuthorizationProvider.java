package io.github.dbchoi85.pmiai.auth;

public interface AuthorizationProvider<T> {
    T issue();
    boolean verify(T credential);
    int credentialSize(T credential);
    long authorityInteractions();
}
