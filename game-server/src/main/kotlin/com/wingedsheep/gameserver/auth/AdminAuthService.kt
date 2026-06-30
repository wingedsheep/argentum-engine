package com.wingedsheep.gameserver.auth

import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.gameserver.persistence.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

/**
 * The single authority for "may this request use the admin dashboard". There are two ways in:
 *
 *  1. **Bootstrap password** — the `X-Admin-Password` header matching `game.admin.password`. This is
 *     how the very first admin gets in (and a break-glass path that always works), but it isn't tied
 *     to any account, so it can't be the long-term answer for a team.
 *  2. **Admin account** — a normal `Authorization: Bearer <token>` whose account has `is_admin = true`.
 *     Admins are promoted from the dashboard's Players view, so after the one-time bootstrap nobody has
 *     to share the password.
 *
 * The admin-account check is resolved against the database per request (not baked into the token) so a
 * promotion or demotion takes effect immediately rather than on the next sign-in. Admin endpoints are
 * low-traffic, so the extra lookup is cheap.
 *
 * This component is intentionally NOT gated on `accounts.enabled` so the password path keeps working
 * on a server with no database at all. When accounts are disabled the account dependencies below are
 * simply absent, leaving the password as the only path.
 */
@Component
class AdminAuthService(private val gameProperties: GameProperties) {

    /** Present only when accounts are enabled (gated beans). Null otherwise — password is then the only path. */
    @Autowired(required = false)
    var authSupport: AuthSupport? = null

    @Autowired(required = false)
    var users: UserRepository? = null

    /** True if the request carries valid admin credentials by either path. */
    fun isAuthorized(password: String?, authorization: String?): Boolean {
        val configured = gameProperties.admin.password
        if (configured.isNotBlank() && password != null && constantTimeEquals(password, configured)) return true

        val claims = authSupport?.userOrNull(authorization) ?: return false
        val user = users?.findById(claims.userId)?.orElse(null) ?: return false
        return user.isAdmin
    }

    /** Whether an admin path even exists on this server (a password is set, or accounts are enabled). */
    fun isConfigured(): Boolean = gameProperties.admin.password.isNotBlank() || authSupport != null

    /**
     * Guard helper for controllers: returns `null` when authorized, or the appropriate 401 response
     * otherwise — `block()` only runs when authorized. Mirrors the shape the admin controllers used
     * before this service existed.
     */
    fun guard(password: String?, authorization: String?, block: () -> ResponseEntity<Any>): ResponseEntity<Any> {
        if (!isConfigured()) {
            return ResponseEntity.status(401).body(mapOf("error" to "Admin feature is not configured"))
        }
        if (!isAuthorized(password, authorization)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Invalid admin credentials"))
        }
        return block()
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}
