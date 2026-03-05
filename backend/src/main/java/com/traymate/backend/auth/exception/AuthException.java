// package com.traymate.backend.auth.exception;

// import org.springframework.http.HttpStatus;
// import org.springframework.web.bind.annotation.ResponseStatus;

// @ResponseStatus(HttpStatus.UNAUTHORIZED)
// public class AuthException extends RuntimeException {

//     public AuthException(String message) {
//         super(message);
//     }
// }

package com.traymate.backend.auth.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
