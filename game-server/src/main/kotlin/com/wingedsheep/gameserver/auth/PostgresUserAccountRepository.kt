package com.wingedsheep.gameserver.auth

import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Production [UserAccountRepository] backed by Postgres via Spring Data JPA. Thin delegating
 * wrapper around [UserAccountJpaRepository] so callers stay decoupled from the JPA types and
 * the storage decision is reversible (see backlog/oauth2-accounts.md "Persistence approach").
 */
@Component
class PostgresUserAccountRepository(
    private val jpa: UserAccountJpaRepository
) : UserAccountRepository {

    override fun findById(id: UUID): UserAccount? =
        jpa.findById(id).orElse(null)

    override fun findByProviderAndSubject(provider: String, subject: String): UserAccount? =
        jpa.findByProviderAndSubject(provider, subject)

    override fun findByDisplayNameIgnoreCaseAndDiscriminator(
        displayName: String,
        discriminator: Short
    ): UserAccount? = jpa.findByDisplayNameIgnoreCaseAndDiscriminator(displayName, discriminator)

    override fun findDiscriminatorsInUseFor(displayName: String): Set<Short> =
        jpa.findDiscriminatorsInUseFor(displayName).toSet()

    override fun save(account: UserAccount): UserAccount = jpa.save(account)
}
