package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kami of the Painted Road (CHK #23) — "Whenever you cast a Spirit or Arcane spell, this creature
 * gains protection from the color of your choice until end of turn."
 */
class KamiOfThePaintedRoadScenarioTest : ScenarioTestBase() {

    private fun board(spell: String, land: String, count: Int) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Kami of the Painted Road")
        .withCardInHand(1, spell)
        .withLandsOnBattlefield(1, land, count)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Kami of the Painted Road") {

            test("casting an Arcane spell grants protection from the chosen colour until end of turn") {
                val game = board("Ethereal Haze", "Plains", 1)
                val kami = game.findPermanent("Kami of the Painted Road")!!

                game.castSpell(1, "Ethereal Haze").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseColorDecision>()
                game.submitDecision(ColorChosenResponse(decision.id, Color.RED))
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("Protection from the chosen colour only") {
                    projected.hasKeyword(kami, "PROTECTION_FROM_RED") shouldBe true
                    projected.hasKeyword(kami, "PROTECTION_FROM_BLACK") shouldBe false
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("Until end of turn — gone by the next upkeep") {
                    game.state.projectedState.hasKeyword(kami, "PROTECTION_FROM_RED") shouldBe false
                }
            }

            test("casting a Spirit spell triggers it") {
                val game = board("Kami of the Hunt", "Forest", 3)
                val kami = game.findPermanent("Kami of the Painted Road")!!

                game.castSpell(1, "Kami of the Hunt").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseColorDecision>()
                game.submitDecision(ColorChosenResponse(decision.id, Color.BLACK))
                game.resolveStack()

                game.state.projectedState.hasKeyword(kami, "PROTECTION_FROM_BLACK") shouldBe true
            }

            test("a non-Spirit, non-Arcane spell does not trigger it") {
                val game = board("Grizzly Bears", "Forest", 2)
                val kami = game.findPermanent("Kami of the Painted Road")!!

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("No colour prompt") { game.hasPendingDecision() shouldBe false }
                Color.entries.forEach { c ->
                    game.state.projectedState.hasKeyword(kami, "PROTECTION_FROM_${c.name}") shouldBe false
                }
            }
        }
    }
}
