package com.wingedsheep.gameserver.persistence

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.flywaydb.core.Flyway
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

/**
 * Proves the migrations are valid PostgreSQL and the schema supports the account/deck/stats/friends
 * round-trips — including the win-count query backing [MatchResultRepository.countWinsForUser], the
 * V4 BIGINT→UUID account-id swap (which must preserve existing rows and re-point every foreign key),
 * and the V5 friends schema.
 *
 * Self-skips when Docker is unavailable, so CI/dev boxes without Docker still pass.
 */
class FlywayMigrationTest : FunSpec({

    val dockerAvailable = runCatching { DockerClientFactory.instance().isDockerAvailable }.getOrDefault(false)

    // A fixed account UUID for the round-trip tests (users.id is a UUID from V4 on).
    val alice = "11111111-1111-1111-1111-111111111111"

    fun migrateAll(postgres: PostgreSQLContainer<Nothing>) {
        Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    test("migrations apply and support account/deck/stats round-trips").config(enabled = dockerAvailable) {
        val postgres = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16-alpine"))
        postgres.start()
        try {
            migrateAll(postgres)

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
                conn.createStatement().use { st ->
                    st.executeQuery(
                        """
                        SELECT count(*) FROM information_schema.tables
                        WHERE table_name IN ('users','login_tokens','decks','match_results','match_participants')
                        """.trimIndent()
                    ).use { rs ->
                        rs.next()
                        rs.getInt(1) shouldBe 5
                    }

                    st.execute("INSERT INTO users(id, email, display_name) VALUES ('$alice', 'a@test.com', 'a')")
                    st.execute("INSERT INTO decks(user_id, name, format, data) VALUES ('$alice', 'My Deck', 'STANDARD', '{}')")
                    st.execute("INSERT INTO match_results(id, game_id) VALUES (10, 'g1')")
                    st.execute("INSERT INTO match_participants(match_id, user_id, player_name, won) VALUES (10, '$alice', 'a', true)")
                    st.execute("INSERT INTO match_participants(match_id, user_id, player_name, won) VALUES (10, NULL, 'guest', false)")

                    st.executeQuery("SELECT count(*) FROM match_participants WHERE user_id = '$alice' AND won = true").use { rs ->
                        rs.next()
                        rs.getInt(1) shouldBe 1
                    }
                    st.executeQuery("SELECT count(*) FROM decks WHERE user_id = '$alice'").use { rs ->
                        rs.next()
                        rs.getInt(1) shouldBe 1
                    }
                }
            }
        } finally {
            postgres.stop()
        }
    }

    test("V2 migration adds the stats schema and supports its aggregate queries").config(enabled = dockerAvailable) {
        val postgres = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16-alpine"))
        postgres.start()
        try {
            migrateAll(postgres)

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
                conn.createStatement().use { st ->
                    // New tables exist.
                    st.executeQuery(
                        """
                        SELECT count(*) FROM information_schema.tables
                        WHERE table_name IN ('match_participant_cards','tournaments','tournament_participants')
                        """.trimIndent()
                    ).use { rs -> rs.next(); rs.getInt(1) shouldBe 3 }

                    // A signed-in winner (WU) vs a guest, with deck cards.
                    st.execute("INSERT INTO users(id, email, display_name) VALUES ('$alice', 'a@test.com', 'Alice')")
                    st.execute("INSERT INTO match_results(id, game_id, game_mode, frame_count, turn_count) VALUES (10, 'g1', 'CASUAL', 22, 7)")
                    st.execute("INSERT INTO match_participants(id, match_id, user_id, player_name, won, colors, set_codes, is_ai, client_ip) VALUES (100, 10, '$alice', 'Alice', true, 'WU', 'DSK,BLB', false, '8.8.8.8')")
                    st.execute("INSERT INTO match_participants(id, match_id, user_id, player_name, won, colors, set_codes, is_ai) VALUES (101, 10, NULL, 'Guest', false, 'R', 'DSK', false)")
                    st.execute("INSERT INTO match_participant_cards(participant_id, card_name, copies) VALUES (100, 'Island', 9), (100, 'Plains', 8), (101, 'Mountain', 17)")

                    // set_codes splitting (unnest + string_to_array).
                    st.executeQuery(
                        """
                        SELECT count(*) FROM match_participants p
                        CROSS JOIN LATERAL unnest(string_to_array(p.set_codes, ',')) AS s
                        WHERE p.user_id = '$alice'
                        """.trimIndent()
                    ).use { rs -> rs.next(); rs.getInt(1) shouldBe 2 }

                    // Card win rate (FILTER): Island appears in one deck, which won.
                    st.executeQuery(
                        """
                        SELECT count(*) FILTER (WHERE pp.won) FROM match_participant_cards c
                        JOIN match_participants pp ON pp.id = c.participant_id
                        WHERE c.card_name = 'Island'
                        """.trimIndent()
                    ).use { rs -> rs.next(); rs.getInt(1) shouldBe 1 }

                    // Games-per-day bucketing.
                    st.executeQuery(
                        "SELECT count(*) FROM match_results WHERE ended_at >= now() - (30 * interval '1 day')"
                    ).use { rs -> rs.next(); rs.getInt(1) shouldBe 1 }

                    // Tournament round-trip with a placement.
                    st.execute("INSERT INTO tournaments(id, lobby_id, name, format, game_mode, player_count, winner_name) VALUES (200, 'lob1', 'Sealed', 'SEALED', 'TOURNAMENT', 4, 'Alice')")
                    st.execute("INSERT INTO tournament_participants(tournament_id, user_id, player_name, is_ai, placement, wins, losses, draws) VALUES (200, '$alice', 'Alice', false, 1, 3, 0, 0)")
                    st.executeQuery("SELECT placement FROM tournament_participants WHERE user_id = '$alice'").use { rs ->
                        rs.next(); rs.getInt(1) shouldBe 1
                    }

                    // Deleting a match cascades to its cards.
                    st.execute("DELETE FROM match_results WHERE id = 10")
                    st.executeQuery("SELECT count(*) FROM match_participant_cards").use { rs ->
                        rs.next(); rs.getInt(1) shouldBe 0
                    }
                }
            }
        } finally {
            postgres.stop()
        }
    }

    test("V5 migration adds game_replays and supports the history replay join").config(enabled = dockerAvailable) {
        val postgres = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16-alpine"))
        postgres.start()
        try {
            migrateAll(postgres)

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
                conn.createStatement().use { st ->
                    st.executeQuery(
                        "SELECT count(*) FROM information_schema.tables WHERE table_name = 'game_replays'"
                    ).use { rs -> rs.next(); rs.getInt(1) shouldBe 1 }

                    // One user with two games; only one has a stored replay.
                    st.execute("INSERT INTO users(id, email, display_name) VALUES ('$alice', 'a@test.com', 'Alice')")
                    st.execute("INSERT INTO match_results(id, game_id) VALUES (10, 'g-with-replay')")
                    st.execute("INSERT INTO match_results(id, game_id) VALUES (11, 'g-no-replay')")
                    st.execute("INSERT INTO match_participants(match_id, user_id, player_name, won) VALUES (10, '$alice', 'Alice', true)")
                    st.execute("INSERT INTO match_participants(match_id, user_id, player_name, won) VALUES (11, '$alice', 'Alice', false)")
                    st.execute("INSERT INTO game_replays(game_id, data) VALUES ('g-with-replay', 'GZIPPED')")

                    // game_id is unique (upsert depends on it).
                    runCatching {
                        st.execute("INSERT INTO game_replays(game_id, data) VALUES ('g-with-replay', 'DUP')")
                    }.isFailure shouldBe true

                    // The history LEFT JOIN flags exactly the game that has a stored replay.
                    st.executeQuery(
                        """
                        SELECT r.game_id, (gr.id IS NOT NULL) AS has_replay
                        FROM match_participants me
                        JOIN match_results r ON r.id = me.match_id
                        LEFT JOIN game_replays gr ON gr.game_id = r.game_id
                        WHERE me.user_id = '$alice'
                        ORDER BY r.game_id
                        """.trimIndent()
                    ).use { rs ->
                        rs.next(); rs.getString("game_id") shouldBe "g-no-replay"; rs.getBoolean("has_replay") shouldBe false
                        rs.next(); rs.getString("game_id") shouldBe "g-with-replay"; rs.getBoolean("has_replay") shouldBe true
                    }
                }
            }
        } finally {
            postgres.stop()
        }
    }

    test("V6 migrates BIGINT account ids to UUID, preserving rows and foreign keys").config(enabled = dockerAvailable) {
        val postgres = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16-alpine"))
        postgres.start()
        try {
            // Bring the schema up to V3 — the BIGINT-id era — and seed legacy data.
            Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration")
                .target("3")
                .load()
                .migrate()

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
                conn.createStatement().use { st ->
                    st.execute("INSERT INTO users(id, email, display_name) VALUES (1, 'a@test.com', 'Alice'), (2, 'b@test.com', 'Bob')")
                    st.execute("INSERT INTO login_tokens(user_id, token_hash, expires_at) VALUES (1, 'h1', now() + interval '1 hour')")
                    st.execute("INSERT INTO decks(user_id, name, data) VALUES (1, 'Deck', '{}')")
                    st.execute("INSERT INTO match_results(id, game_id) VALUES (10, 'g1')")
                    st.execute("INSERT INTO match_participants(match_id, user_id, player_name, won) VALUES (10, 1, 'Alice', true), (10, 2, 'Bob', false)")
                }
            }

            // Now run the remaining migrations, including the V6 UUID swap.
            migrateAll(postgres)

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
                conn.createStatement().use { st ->
                    // users.id is now uuid, and both rows survived.
                    st.executeQuery("SELECT data_type FROM information_schema.columns WHERE table_name='users' AND column_name='id'")
                        .use { rs -> rs.next(); rs.getString(1) shouldBe "uuid" }
                    st.executeQuery("SELECT count(*) FROM users").use { rs -> rs.next(); rs.getInt(1) shouldBe 2 }

                    // The deck still resolves to Alice through the re-pointed UUID foreign key.
                    st.executeQuery(
                        "SELECT u.email FROM decks d JOIN users u ON u.id = d.user_id WHERE d.name = 'Deck'"
                    ).use { rs -> rs.next(); rs.getString(1) shouldBe "a@test.com" }

                    // The login token and both match seats are re-pointed too.
                    st.executeQuery(
                        "SELECT u.email FROM login_tokens t JOIN users u ON u.id = t.user_id"
                    ).use { rs -> rs.next(); rs.getString(1) shouldBe "a@test.com" }
                    st.executeQuery(
                        "SELECT count(*) FROM match_participants p JOIN users u ON u.id = p.user_id"
                    ).use { rs -> rs.next(); rs.getInt(1) shouldBe 2 }

                    // FK rules survive the swap: deleting Alice cascades her deck and nulls her seat.
                    st.execute("DELETE FROM users WHERE email = 'a@test.com'")
                    st.executeQuery("SELECT count(*) FROM decks").use { rs -> rs.next(); rs.getInt(1) shouldBe 0 }
                    st.executeQuery("SELECT count(*) FROM match_participants WHERE user_id IS NULL").use { rs ->
                        rs.next(); rs.getInt(1) shouldBe 1
                    }
                }
            }
        } finally {
            postgres.stop()
        }
    }

    test("V7 friends schema supports the request/accept round-trip and cascades").config(enabled = dockerAvailable) {
        val postgres = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16-alpine"))
        postgres.start()
        try {
            migrateAll(postgres)
            val bob = "22222222-2222-2222-2222-222222222222"

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
                conn.createStatement().use { st ->
                    // A UUID-PK insert with no id should be backfilled by the gen_random_uuid() default.
                    st.execute("INSERT INTO users(email, display_name) VALUES ('gen@test.com', 'Gen')")
                    st.executeQuery("SELECT id FROM users WHERE email = 'gen@test.com'").use { rs ->
                        rs.next(); rs.getString("id") shouldNotBe null
                    }

                    st.execute("INSERT INTO users(id, email, display_name, hide_presence) VALUES ('$alice', 'a@test.com', 'Alice', false)")
                    st.execute("INSERT INTO users(id, email, display_name) VALUES ('$bob', 'b@test.com', 'Bob')")

                    // Alice requests Bob; it starts PENDING.
                    st.execute("INSERT INTO friendships(requester_id, addressee_id) VALUES ('$alice', '$bob')")
                    st.executeQuery(
                        "SELECT status FROM friendships WHERE requester_id = '$alice' AND addressee_id = '$bob'"
                    ).use { rs -> rs.next(); rs.getString(1) shouldBe "PENDING" }

                    // The symmetric pair lookup finds it from either direction.
                    st.executeQuery(
                        """
                        SELECT count(*) FROM friendships
                        WHERE (requester_id = '$bob' AND addressee_id = '$alice')
                           OR (requester_id = '$alice' AND addressee_id = '$bob')
                        """.trimIndent()
                    ).use { rs -> rs.next(); rs.getInt(1) shouldBe 1 }

                    // Accept it.
                    st.execute("UPDATE friendships SET status = 'ACCEPTED', responded_at = now() WHERE requester_id = '$alice'")

                    // Deleting an account cascades its friendships.
                    st.execute("DELETE FROM users WHERE id = '$bob'")
                    st.executeQuery("SELECT count(*) FROM friendships").use { rs -> rs.next(); rs.getInt(1) shouldBe 0 }
                }
            }
        } finally {
            postgres.stop()
        }
    }

    test("V9 adds tournament status and allows null ended_at for in-progress rows").config(enabled = dockerAvailable) {
        val postgres = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16-alpine"))
        postgres.start()
        try {
            migrateAll(postgres)

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
                conn.createStatement().use { st ->
                    // An insert that omits status backfills status=COMPLETED (the column default).
                    st.execute(
                        "INSERT INTO tournaments(id, lobby_id, name, player_count, ended_at) " +
                            "VALUES (300, 'done', 'Finished', 4, now())"
                    )
                    st.executeQuery("SELECT status FROM tournaments WHERE id = 300").use { rs ->
                        rs.next(); rs.getString(1) shouldBe "COMPLETED"
                    }

                    // An in-progress tournament: null ended_at, explicit status and started_at.
                    st.execute(
                        "INSERT INTO tournaments(id, lobby_id, name, player_count, status, started_at, ended_at) " +
                            "VALUES (301, 'live', 'Running', 4, 'IN_PROGRESS', now(), NULL)"
                    )
                    st.executeQuery("SELECT status, ended_at FROM tournaments WHERE id = 301").use { rs ->
                        rs.next(); rs.getString(1) shouldBe "IN_PROGRESS"; rs.getTimestamp(2) shouldBe null
                    }

                    // Ordering by COALESCE(ended_at, started_at) surfaces both, newest first.
                    st.executeQuery(
                        "SELECT id FROM tournaments ORDER BY COALESCE(ended_at, started_at, now()) DESC"
                    ).use { rs ->
                        rs.next(); val first = rs.getLong(1)
                        rs.next(); val second = rs.getLong(1)
                        setOf(first, second) shouldBe setOf(300L, 301L)
                    }
                }
            }
        } finally {
            postgres.stop()
        }
    }
})
