package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.sidesFor
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Phase 3 of `backlog/engine-ai-improvement.md`: the evaluator away from a two-player table.
 *
 * Every feature in `BoardFeatures.kt` used to open with `soleOpponent(playerId)` — literally
 * `getOpponents(playerId).firstOrNull()`. Contrary to the plan's diagnosis that this returned null
 * and zeroed the evaluator, it returned the **first opponent in turn order**, so the AI evaluated a
 * pod as if it were a two-player game against one arbitrary neighbour: blind to every other
 * opponent, blind to its own teammate, and reading a per-player `LifeTotalComponent` that the
 * engine stops maintaining once Two-Headed Giant pools life on the team's canonical owner.
 *
 * These are the claims that were false before the change and are true after it. The 1v1 no-op half
 * is asserted where it belongs — `FrozenBaselineTest` hashes a whole V0 game, and `PuzzleSuiteTest`
 * pins all 48 puzzle verdicts.
 */
class MultiplayerEvaluationTest : FunSpec({

    val registry = CardRegistry().apply { register(TestCards.all) }
    val evaluator = EvaluationWeights.DEFAULT.toEvaluator()

    fun boot(seats: Int, format: Format = Format.Standard, teams: List<List<Int>>? = null): GameState =
        GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..seats).map { PlayerConfig("P$it", Deck.of("Forest" to 30, "Grizzly Bears" to 10)) },
                skipMulligans = true,
                startingPlayerIndex = 0, // keeps turnOrder aligned with the configured seat order
                format = format,
                teams = teams,
                seed = 424242L,
            )
        ).state

    /** Move a Grizzly Bears out of [playerId]'s library and onto the battlefield under their control. */
    fun GameState.withBear(playerId: EntityId): GameState {
        val cardId = getZone(playerId, Zone.LIBRARY).first {
            getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
        }
        return removeFromZone(ZoneKey(playerId, Zone.LIBRARY), cardId)
            .addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), cardId)
            .updateEntity(cardId) { it.with(ControllerComponent(playerId)) }
    }

    fun GameState.score(playerId: EntityId): Double = evaluator.evaluate(this, projectedState, playerId)

    // ── Free-for-all ─────────────────────────────────────────────────────────

    test("a three-player pod has two opposing sides, not one") {
        val state = boot(3)
        val (me, left, right) = state.turnOrder
        val sides = state.sidesFor(me)!!

        sides.mine shouldBe listOf(me)
        sides.opponents shouldBe listOf(listOf(left), listOf(right))
    }

    test("the second opponent is visible — the whole point of Phase 3") {
        // The old `soleOpponent` picked the first opponent in turn order and never looked at the
        // rest, so a board this size appearing on the *far* side of the table moved the score by
        // exactly 0.0.
        val base = boot(3)
        val (me, _, right) = base.turnOrder

        val armed = (1..4).fold(base) { s, _ -> s.withBear(right) }

        withClue("four 2/2s on the third seat's board must lower our evaluation") {
            armed.score(me) shouldBeLessThan base.score(me)
        }
    }

    test("the runaway leader dominates, but every opponent still has a gradient") {
        // THREAT is a blend of the worst matchup and the field mean precisely so this holds: a
        // pure `min` would score the weak opponent's board at exactly zero and leave the AI with
        // no reason ever to interact with them.
        //
        // Three creatures against two, not four against one: emptying an opponent's board entirely
        // flips ThreatAssessment's "turns until they kill me" from a real number to its 99-turn
        // sentinel, a ~130-point discontinuity that swamps any comparison across it. That cliff is
        // a pre-existing hand-tuned constant (Phase 9's job), not something this fold controls.
        val base = boot(3)
        val (me, leader, weak) = base.turnOrder
        val table = (1..3).fold(base) { s, _ -> s.withBear(leader) }
            .let { (1..2).fold(it) { s, _ -> s.withBear(weak) } }

        val killedOnLeader = table.removeBear(leader)
        val killedOnWeak = table.removeBear(weak)

        withClue("killing the leader's creature must be worth more than the weak seat's") {
            killedOnLeader.score(me) shouldBeGreaterThan killedOnWeak.score(me)
        }
        withClue("killing the weak seat's creature must still be worth something") {
            killedOnWeak.score(me) shouldBeGreaterThan table.score(me)
        }
    }

    test("an eliminated player scores a loss even while the game continues (CR 104.3b)") {
        val base = boot(3)
        val (me, left, _) = base.turnOrder
        val eliminated = base.updateEntity(me) { it.with(PlayerLostComponent(LossReason.LIFE_ZERO)) }

        withClue("we are out; the surviving opponents' boards must not be scored as our position") {
            eliminated.score(me) shouldBeLessThan -1_000_000.0
        }
        withClue("the other seats are unaffected") {
            eliminated.score(left) shouldBeGreaterThan -1_000_000.0
        }
    }

    // ── Two-Headed Giant (CR 810) ────────────────────────────────────────────

    val twoHeaded = Format.TwoHeadedGiant()
    val teams = listOf(listOf(0, 1), listOf(2, 3))

    test("a 2HG table is two sides of two, and my teammate is on mine") {
        val state = boot(4, twoHeaded, teams)
        val (me, partner, foeA, foeB) = state.turnOrder
        val sides = state.sidesFor(me)!!

        sides.mine shouldBe listOf(me, partner)
        sides.opponents shouldBe listOf(listOf(foeA, foeB))
    }

    test("my teammate's board counts as mine") {
        val base = boot(4, twoHeaded, teams)
        val (me, partner, _, _) = base.turnOrder

        withClue("a creature on my partner's side of the table defends me too (CR 805.10)") {
            base.withBear(partner).score(me) shouldBeGreaterThan base.score(me)
        }
    }

    test("life reads the team's shared pool, not the stale per-player component") {
        // The team's life lives on its canonical owner (GameState.teamLifeOwnerOf). The
        // non-canonical member's own LifeTotalComponent is never written again after setup, so
        // reading it directly froze the life differential at the starting 30 for half the table.
        val base = boot(4, twoHeaded, teams)
        val (me, partner, foeA, _) = base.turnOrder
        val damaged = base.adjustLife(me, -20)

        withClue("both teammates see the same pool") {
            damaged.lifeTotal(me) shouldBe damaged.lifeTotal(partner)
            damaged.lifeTotal(me) shouldBe 10
        }
        withClue("dropping to 10 must be visible from the non-canonical seat as well") {
            damaged.score(partner) shouldBeLessThan base.score(partner)
        }
        withClue("and it must look good from the other team") {
            damaged.score(foeA) shouldBeGreaterThan base.score(foeA)
        }
    }

    test("a teammate's win is my win") {
        // GameEndCheck records one representative of the winning team in winnerId. Comparing it to
        // playerId directly scored the other member of every won team game as a total loss.
        val base = boot(4, twoHeaded, teams)
        val (me, partner, foeA, _) = base.turnOrder
        val won = base.copy(gameOver = true, winnerId = partner)

        won.score(me) shouldBeGreaterThan 1_000_000.0
        won.score(partner) shouldBeGreaterThan 1_000_000.0
        won.score(foeA) shouldBeLessThan -1_000_000.0
    }
})

/** Remove one of [playerId]'s battlefield creatures — "our removal spell resolved". */
private fun GameState.removeBear(playerId: EntityId): GameState {
    val cardId = getZone(playerId, Zone.BATTLEFIELD).first {
        getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
    }
    return removeFromZone(ZoneKey(playerId, Zone.BATTLEFIELD), cardId)
}
