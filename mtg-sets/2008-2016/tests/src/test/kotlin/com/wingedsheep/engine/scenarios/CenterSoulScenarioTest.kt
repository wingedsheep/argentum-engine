package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Center Soul (DTK #8) — {1}{W} Instant.
 *
 * "Target creature you control gains protection from the color of your choice until end of turn.
 * Rebound"
 *
 * Pins that the chosen colour (and only it) becomes protection on the target, that a hand-cast
 * Center Soul is exiled by rebound rather than going to the graveyard, and that an opponent's
 * creature is not a legal target.
 */
class CenterSoulScenarioTest : ScenarioTestBase() {

    init {
        context("Center Soul") {

            test("grants protection from the chosen color and rebounds into exile") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Center Soul")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Center Soul", bears).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseColorDecision>()
                game.submitDecision(ColorChosenResponse(decision.id, Color.RED)).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.hasKeyword(bears, "PROTECTION_FROM_RED") shouldBe true
                projected.hasKeyword(bears, "PROTECTION_FROM_BLACK") shouldBe false
                withClue("cast from hand, rebound exiles it instead of putting it in the graveyard") {
                    game.isInExile(1, "Center Soul") shouldBe true
                    game.isInGraveyard(1, "Center Soul") shouldBe false
                }
            }

            test("an opponent's creature is not a legal target") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Center Soul")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val theirBears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Center Soul", theirBears).error shouldNotBe null
            }
        }
    }
}
