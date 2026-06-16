package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.TeamComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Two-Headed Giant — Phase 1: the format config + team model.
 *
 * Proves the foundation only (CR 810.1 / 810.4): a four-seat game boots into two teams of two,
 * each player carries the right [TeamComponent], the team starts at 30 shared-format life, and the
 * [com.wingedsheep.engine.state.GameState] team helpers group/iterate by team. Shared life, shared
 * turns/priority, combined combat, and team win/loss are later phases and are *not* asserted here.
 *
 * Also pins the no-team degradation: in a Standard game every player is its own team with no
 * component, so the team helpers behave per-player and existing games are unaffected.
 */
class TwoHeadedGiantSetupTest : FunSpec({

    fun registry(): CardRegistry {
        val r = CardRegistry()
        r.register(TestCards.all)
        return r
    }

    fun boot2hg(teams: List<List<Int>>? = listOf(listOf(0, 1), listOf(2, 3))) =
        GameInitializer(registry()).initializeGame(
            GameConfig(
                format = Format.TwoHeadedGiant(),
                players = (1..4).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                teams = teams,
                startingPlayerIndex = 0, // keep turnOrder == playerIds for deterministic assertions
                skipMulligans = true,
            )
        )

    test("a 2HG game boots into two teams of two at the shared 30-life starting value") {
        val result = boot2hg()
        val players = result.playerIds

        (result.state.format is Format.TwoHeadedGiant) shouldBe true
        players.size shouldBe 4

        // CR 810.4 — each player starts at the team's 30 (shared pool itself is a later phase).
        for (pid in players) {
            result.state.getEntity(pid)!!.get<LifeTotalComponent>()!!.life shouldBe 30
        }

        // Team indices stamped per config.teams = [[0,1],[2,3]].
        result.state.getEntity(players[0])!!.get<TeamComponent>()!!.teamIndex shouldBe 0
        result.state.getEntity(players[1])!!.get<TeamComponent>()!!.teamIndex shouldBe 0
        result.state.getEntity(players[2])!!.get<TeamComponent>()!!.teamIndex shouldBe 1
        result.state.getEntity(players[3])!!.get<TeamComponent>()!!.teamIndex shouldBe 1
    }

    test("teams / teamOf / teammatesOf group players by their team in turn order") {
        val result = boot2hg()
        val players = result.playerIds
        val state = result.state

        state.teams shouldContainExactly listOf(
            listOf(players[0], players[1]),
            listOf(players[2], players[3]),
        )
        state.teamOf(players[0]) shouldContainExactly listOf(players[0], players[1])
        state.teamOf(players[3]) shouldContainExactly listOf(players[2], players[3])
        state.teammatesOf(players[0]) shouldContainExactly listOf(players[1])
        state.teammatesOf(players[2]) shouldContainExactly listOf(players[3])
    }

    test("teams grouping preserves turn order even when teammates are not adjacent seats") {
        // Cross-paired teams: seats 0 & 2 vs seats 1 & 3.
        val result = boot2hg(teams = listOf(listOf(0, 2), listOf(1, 3)))
        val players = result.playerIds

        // Teams appear in the order their first member appears in turnOrder; members keep turn order.
        result.state.teams shouldContainExactly listOf(
            listOf(players[0], players[2]),
            listOf(players[1], players[3]),
        )
        result.state.teamOf(players[2]) shouldContainExactly listOf(players[0], players[2])
    }

    test("teamActivePlayers excludes a teammate who has lost the game") {
        val result = boot2hg()
        val players = result.playerIds
        val withLoss = result.state.updateEntity(players[1]) { c ->
            c.with(PlayerLostComponent(LossReason.CONCESSION))
        }

        withLoss.teamActivePlayers(players[0]) shouldContainExactly listOf(players[0])
        // teamOf still includes the lost teammate (it tracks membership, not liveness).
        withLoss.teamOf(players[0]) shouldContainExactly listOf(players[0], players[1])
    }

    test("no-team games degrade to one singleton team per player") {
        // Standard format, no teams config — the default for every existing game.
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                players = (1..4).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        val players = result.playerIds
        val state = result.state

        for (pid in players) state.getEntity(pid)!!.get<TeamComponent>() shouldBe null
        state.teams shouldContainExactly players.map { listOf(it) }
        state.teamOf(players[0]) shouldContainExactly listOf(players[0])
        state.teammatesOf(players[0]) shouldContainExactly emptyList()
        state.teamActivePlayers(players[0]) shouldContainExactly listOf(players[0])
    }

    test("a teams config that does not partition every player exactly once is rejected") {
        // Player index 3 is missing, and index 1 is duplicated.
        shouldThrow<IllegalArgumentException> {
            GameInitializer(registry()).initializeGame(
                GameConfig(
                    format = Format.TwoHeadedGiant(),
                    players = (1..4).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                    teams = listOf(listOf(0, 1), listOf(1, 2)),
                    skipMulligans = true,
                )
            )
        }
    }
})
