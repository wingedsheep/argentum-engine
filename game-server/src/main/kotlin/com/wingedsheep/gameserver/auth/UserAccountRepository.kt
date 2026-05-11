package com.wingedsheep.gameserver.auth

import java.util.UUID

/**
 * Storage abstraction for [UserAccount]. Wraps the underlying persistence (currently
 * Spring Data JPA against Postgres, see [PostgresUserAccountRepository]) so the
 * Postgres ↔ Redis decision documented in backlog/oauth2-accounts.md stays localized
 * to a single bean swap.
 */
interface UserAccountRepository {

    /** Find an account by its server-issued UUID. */
    fun findById(id: UUID): UserAccount?

    /**
     * Find by `(provider, subject)` — the natural OAuth identity key. Used at every
     * sign-in to decide whether to JIT-provision a fresh row or update an existing one.
     */
    fun findByProviderAndSubject(provider: String, subject: String): UserAccount?

    /**
     * Find by full handle — case-insensitive on display name + exact match on discriminator.
     * Used by `/api/users/by-handle/{handle}` and head-to-head deep links.
     */
    fun findByDisplayNameIgnoreCaseAndDiscriminator(displayName: String, discriminator: Short): UserAccount?

    /**
     * Return the set of discriminators currently in use for the given display name
     * (case-insensitively). Used by [DiscriminatorAllocator] to pick the next free slot.
     */
    fun findDiscriminatorsInUseFor(displayName: String): Set<Short>

    /** Insert or update. */
    fun save(account: UserAccount): UserAccount
}
