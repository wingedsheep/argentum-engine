package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Luminous Broodmoth (IKO) — "Whenever a creature you control without flying dies, return it to
 * the battlefield under its owner's control with a flying counter on it."
 *
 * Pins the three things the wording turns on: a flightless creature comes back with a flying
 * counter (and so has flying), the returned creature — now a flyer — stays dead the second time,
 * and a creature that already had flying never triggers it.
 */
class LuminousBroodmothScenarioTest : ScenarioTestBase() {

    init {
        test("a flightless creature returns with a flying counter, and stays dead the second time") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Luminous Broodmoth")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                // The second Shock is Player1's own: after Broodmoth's trigger resolves, priority
                // sits with its controller, so they finish off their returned flyer themselves.
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")
            bears.shouldNotBeNull()
            game.castSpell(2, "Shock", bears).error shouldBe null
            game.resolveStack()

            val returned = game.findPermanent("Grizzly Bears")
            withClue("Broodmoth returned the Bears to the battlefield") {
                returned.shouldNotBeNull()
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }
            withClue("it came back with a flying counter, so it has flying") {
                game.state.getEntity(returned!!)?.get<CountersComponent>()
                    ?.getCount(CounterType.FLYING) shouldBe 1
                game.state.projectedState.hasKeyword(returned, Keyword.FLYING) shouldBe true
            }

            game.castSpell(1, "Shock", returned!!).error shouldBe null
            game.resolveStack()

            withClue("now a flyer, its second death doesn't trigger Broodmoth") {
                game.findPermanent("Grizzly Bears") shouldBe null
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }

        test("a creature that already has flying is not returned") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Luminous Broodmoth")
                .withCardOnBattlefield(1, "Wind Drake")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val drake = game.findPermanent("Wind Drake")
            drake.shouldNotBeNull()
            game.castSpell(2, "Shock", drake).error shouldBe null
            game.resolveStack()

            withClue("Wind Drake had flying, so it stays in the graveyard") {
                game.findPermanent("Wind Drake") shouldBe null
                game.isInGraveyard(1, "Wind Drake") shouldBe true
            }
        }
    }
}
