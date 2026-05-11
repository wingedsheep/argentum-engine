package com.wingedsheep.gameserver.auth

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory [UserAccountRepository] used by unit tests (and as the Plan-B fallback documented in
 * backlog/oauth2-accounts.md "Persistence approach"). Not a Spring bean — instantiate directly.
 *
 * Thread-safe enough for concurrent test workloads via [ConcurrentHashMap]; not optimized.
 */
class InMemoryUserAccountRepository : UserAccountRepository {

    private val byId = ConcurrentHashMap<UUID, UserAccount>()

    override fun findById(id: UUID): UserAccount? = byId[id]

    override fun findByProviderAndSubject(provider: String, subject: String): UserAccount? =
        byId.values.firstOrNull { it.provider == provider && it.subject == subject }

    override fun findByDisplayNameIgnoreCaseAndDiscriminator(
        displayName: String,
        discriminator: Short
    ): UserAccount? = byId.values.firstOrNull {
        it.displayName.equals(displayName, ignoreCase = true) && it.discriminator == discriminator
    }

    override fun findDiscriminatorsInUseFor(displayName: String): Set<Short> =
        byId.values
            .filter { it.displayName.equals(displayName, ignoreCase = true) }
            .mapTo(mutableSetOf()) { it.discriminator }

    override fun save(account: UserAccount): UserAccount {
        byId[account.id] = account
        return account
    }
}
