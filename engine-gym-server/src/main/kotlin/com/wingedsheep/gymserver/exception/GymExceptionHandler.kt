package com.wingedsheep.gymserver.exception

import com.wingedsheep.gymserver.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Maps [MultiEnvService][com.wingedsheep.engine.gym.service.MultiEnvService]
 * and engine exceptions onto meaningful HTTP status codes so Python clients
 * can distinguish operator mistakes from server faults:
 *
 *  - `NoSuchElementException` → 404 (unknown env / snapshot)
 *  - `IllegalArgumentException` → 400 (bad deck, invalid action ID, bad config)
 *  - `IllegalStateException` → 409 (decision mismatch, env not paused, etc.)
 *  - anything else → 500
 */
@RestControllerAdvice
class GymExceptionHandler {

    @ExceptionHandler(NoSuchElementException::class)
    fun notFound(ex: NoSuchElementException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("NOT_FOUND", ex.message ?: "Not found"))

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("BAD_REQUEST", ex.message ?: "Bad request"))

    @ExceptionHandler(IllegalStateException::class)
    fun conflict(ex: IllegalStateException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse("CONFLICT", ex.message ?: "Conflict"))
}
