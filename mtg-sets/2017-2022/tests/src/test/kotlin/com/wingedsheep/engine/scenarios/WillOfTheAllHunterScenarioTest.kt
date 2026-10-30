package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Will of the All-Hunter (IKO #38) — {1}{W} Instant.
 *
 *   "Target creature gets +2/+2 until end of turn. If it's blocking, instead put two +1/+1
 *    counters on it."
 *
 * The blocking check reads the target's live combat state on resolution: a blocker gets the
 * counters and *not* the pump; any other creature gets only the pump.
 */
class WillOfTheAllHunterScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Grizzly Bears (P1) attacks, Hill Giant (P2) blocks; P2 casts the spell in declare blockers. */
    private fun inBlockersStep(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(2, "Will of the All-Hunter")
            .withLandsOnBattlefield(2, "Plains", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
        return game
    }

    init {
        test("a blocking creature gets two +1/+1 counters instead of the pump") {
            val game = inBlockersStep()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(2, "Will of the All-Hunter", giant).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant was blocking, so it gets the counters") {
                game.plusOnes(giant) shouldBe 2
            }
            withClue("…and not the +2/+2 on top of them (3/3 base + 2 counters = 5/5)") {
                game.state.projectedState.getPower(giant) shouldBe 5
                game.state.projectedState.getToughness(giant) shouldBe 5
            }
        }

        test("a creature that isn't blocking gets +2/+2 and no counters") {
            val game = inBlockersStep()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(2, "Will of the All-Hunter", bears).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears is attacking, not blocking") {
                game.plusOnes(bears) shouldBe 0
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
            }
        }
    }
}
