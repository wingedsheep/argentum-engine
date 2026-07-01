package com.wingedsheep.gameserver.persistence

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

/**
 * Spring Data JDBC repositories. These beans only exist when accounts are enabled (the JDBC
 * auto-config is @ConditionalOnBean(DataSource), and the DataSource is only present then), so every
 * component that injects one is itself gated on `accounts.enabled`.
 */
interface UserRepository : CrudRepository<UserRow, UUID> {
    fun findByEmail(email: String): UserRow?
}

interface LoginTokenRepository : CrudRepository<LoginTokenRow, Long> {
    fun findByTokenHash(tokenHash: String): LoginTokenRow?
}

interface DeckRepository : CrudRepository<DeckRow, Long> {
    fun findByUserIdOrderByUpdatedAtDesc(userId: UUID): List<DeckRow>
    fun findByIdAndUserId(id: Long, userId: UUID): DeckRow?
    fun deleteByIdAndUserId(id: Long, userId: UUID): Int
}

interface MatchResultRepository : CrudRepository<MatchResultRow, Long> {
    @Query("SELECT count(*) FROM match_participants WHERE user_id = :userId")
    fun countGamesForUser(@Param("userId") userId: UUID): Long

    @Query("SELECT count(*) FROM match_participants WHERE user_id = :userId AND won = true")
    fun countWinsForUser(@Param("userId") userId: UUID): Long
}

interface TournamentRepository : CrudRepository<TournamentRow, Long> {
    /** The most recent row recorded for a lobby, if any — the upsert key for the lifecycle sink. */
    fun findFirstByLobbyIdOrderByIdDesc(lobbyId: String): TournamentRow?
}

/**
 * Friendships and pending friend requests. A friendship is symmetric once accepted, so most lookups
 * check both the requester and addressee columns.
 */
interface FriendshipRepository : CrudRepository<FriendshipRow, UUID> {
    fun findByRequesterIdOrAddresseeId(requesterId: UUID, addresseeId: UUID): List<FriendshipRow>

    @Query(
        """
        SELECT * FROM friendships
        WHERE (requester_id = :a AND addressee_id = :b)
           OR (requester_id = :b AND addressee_id = :a)
        LIMIT 1
        """
    )
    fun findPair(@Param("a") a: UUID, @Param("b") b: UUID): FriendshipRow?
}

interface GameReplayRepository : CrudRepository<GameReplayRow, Long> {
    fun findByGameId(gameId: String): GameReplayRow?
}

interface UserRatingRepository : CrudRepository<UserRatingRow, Long> {
    fun findByUserIdAndMode(userId: UUID, mode: String): UserRatingRow?
    fun findByUserId(userId: UUID): List<UserRatingRow>
}

interface RatingHistoryRepository : CrudRepository<RatingHistoryRow, Long> {
    fun findByUserIdAndModeOrderByCreatedAtAsc(userId: UUID, mode: String): List<RatingHistoryRow>
}
