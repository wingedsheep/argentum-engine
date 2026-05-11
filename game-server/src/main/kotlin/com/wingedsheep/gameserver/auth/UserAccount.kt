package com.wingedsheep.gameserver.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/**
 * Persistent user account, keyed by (provider, subject) — Google's `sub`, GitHub's numeric `id`,
 * Microsoft's `oid`, etc. Each OAuth provider yields a separate account row per human;
 * email-based auto-linking is intentionally not supported (see backlog/oauth2-accounts.md
 * Security Notes).
 *
 * Display names are not unique; the [discriminator] (1..9999, auto-assigned by
 * [DiscriminatorAllocator]) disambiguates collisions, producing handles like `Alice#0042`.
 */
@Entity
@Table(
    name = "user_accounts",
    schema = "auth",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_accounts_provider_subject", columnNames = ["provider", "subject"])
    ]
)
class UserAccount(
    @Id
    val id: UUID,
    @Column(nullable = false, length = 32)
    val provider: String,
    @Column(nullable = false, length = 128)
    val subject: String,
    @Column(nullable = false, length = 320)
    var email: String,
    @Column(name = "display_name", nullable = false, length = 80)
    var displayName: String,
    @Column(nullable = false)
    var discriminator: Short,
    @Column(name = "avatar_url", columnDefinition = "TEXT")
    var avatarUrl: String?,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "last_login_at", nullable = false)
    var lastLoginAt: Instant,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: AccountStatus = AccountStatus.ACTIVE
) {
    /** Stable human-readable handle: `Alice#0042`. */
    val handle: String
        get() = "$displayName#${discriminator.toString().padStart(HANDLE_PAD_WIDTH, '0')}"

    companion object {
        private const val HANDLE_PAD_WIDTH = 4
    }
}

enum class AccountStatus {
    ACTIVE,

    /**
     * Soft-deleted: PII fields (`email`, `displayName`) have been anonymized.
     * The row is kept so foreign keys (e.g. `stats.game_results.opponent_account_id`)
     * don't break for the deleted user's prior opponents.
     */
    DELETED
}
