package com.wingedsheep.gameserver.auth

import com.wingedsheep.gameserver.persistence.UserRepository
import com.wingedsheep.gameserver.persistence.UserRow
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Account-administration mutations performed from the admin dashboard. Kept separate from
 * [MagicLinkService] (which owns the sign-in lifecycle) so the admin-only surface is isolated. Only
 * exists when accounts are enabled — there are no accounts to administer otherwise.
 */
@Service
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class UserAdminService(private val users: UserRepository) {

    fun get(userId: UUID): UserRow? = users.findById(userId).orElse(null)

    /** Grant or revoke admin access for an account. Returns the updated account, or null if unknown. */
    fun setAdmin(userId: UUID, isAdmin: Boolean): UserRow? {
        val user = users.findById(userId).orElse(null) ?: return null
        if (user.isAdmin == isAdmin) return user
        return users.save(user.copy(isAdmin = isAdmin))
    }
}
