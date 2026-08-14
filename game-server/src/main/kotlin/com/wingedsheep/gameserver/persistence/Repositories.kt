package com.wingedsheep.gameserver.persistence

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant
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

interface CubeRepository : CrudRepository<CubeRow, Long> {
    fun findByUserIdOrderByUpdatedAtDesc(userId: UUID): List<CubeRow>
    fun findByIdAndUserId(id: Long, userId: UUID): CubeRow?
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

    /** In-flight recordings to resume after a restart — a handful of rows at most. */
    fun findByStatus(status: String): List<GameReplayRow>

    /**
     * Finished games this seat played in, newest first. Joins the seat index rather than scanning
     * the (large, gzipped) payload column.
     */
    @Query(
        """
        SELECT r.* FROM game_replays r
        JOIN game_replay_players p ON p.replay_id = r.id
        WHERE p.player_id = :playerId AND r.status = 'FINISHED'
        ORDER BY r.ended_at DESC
        LIMIT :limit
        """
    )
    fun findRecentForPlayer(@Param("playerId") playerId: String, @Param("limit") limit: Int): List<GameReplayRow>

    /**
     * The in-progress flush write: only the columns that actually move.
     *
     * Deliberately *not* `save()`. A full aggregate write would name `pinned_cards` (and re-insert the
     * seat children) on every flush, and Postgres only keeps a TOASTed value's storage when an UPDATE
     * leaves that column alone — assigning identical bytes still rewrites it. Naming just the volatile
     * columns is what makes the pins genuinely write-once for the length of a game.
     */
    @Modifying
    @Query(
        """
        UPDATE game_replays
        SET data = :data,
            status = :status,
            resume_fingerprint = :resumeFingerprint,
            frame_count = :frameCount,
            ended_at = :endedAt,
            engine_version = :engineVersion
        WHERE game_id = :gameId
        """
    )
    fun updateRecording(
        @Param("gameId") gameId: String,
        @Param("data") data: String,
        @Param("status") status: String,
        @Param("resumeFingerprint") resumeFingerprint: String?,
        @Param("frameCount") frameCount: Int,
        @Param("endedAt") endedAt: Instant,
        @Param("engineVersion") engineVersion: String?,
    ): Int
}

interface UserRatingRepository : CrudRepository<UserRatingRow, Long> {
    fun findByUserIdAndMode(userId: UUID, mode: String): UserRatingRow?
    fun findByUserId(userId: UUID): List<UserRatingRow>
}

interface RatingHistoryRepository : CrudRepository<RatingHistoryRow, Long> {
    fun findByUserIdAndModeOrderByCreatedAtAsc(userId: UUID, mode: String): List<RatingHistoryRow>
}
