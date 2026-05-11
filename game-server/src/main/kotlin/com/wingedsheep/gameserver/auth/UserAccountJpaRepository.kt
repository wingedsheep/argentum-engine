package com.wingedsheep.gameserver.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

/**
 * Spring Data JPA bindings for [UserAccount]. Application code uses
 * [UserAccountRepository] (via [PostgresUserAccountRepository]) rather than depending
 * on this directly — keeps the storage-engine swap a single-bean change.
 */
interface UserAccountJpaRepository : JpaRepository<UserAccount, UUID> {

    fun findByProviderAndSubject(provider: String, subject: String): UserAccount?

    @Query(
        """
        SELECT a FROM UserAccount a
        WHERE LOWER(a.displayName) = LOWER(:displayName) AND a.discriminator = :discriminator
        """
    )
    fun findByDisplayNameIgnoreCaseAndDiscriminator(
        @Param("displayName") displayName: String,
        @Param("discriminator") discriminator: Short
    ): UserAccount?

    @Query(
        """
        SELECT a.discriminator FROM UserAccount a
        WHERE LOWER(a.displayName) = LOWER(:displayName)
        """
    )
    fun findDiscriminatorsInUseFor(@Param("displayName") displayName: String): List<Short>
}
