package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Failed Conversion (MOM #103) — {4}{B} Enchantment — Aura.
 * "Enchanted creature gets -4/-4. When enchanted creature dies, surveil 2."
 */
class FailedConversionScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Failed Conversion") {

            test("gives the enchanted creature -4/-4") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Colossal Dreadmaw")
                    .withCardAttachedTo(1, "Failed Conversion", "Colossal Dreadmaw")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = projector.project(game.state)
                val id = game.findPermanent("Colossal Dreadmaw")!!
                projected.getPower(id) shouldBe 2
                projected.getToughness(id) shouldBe 2
            }

            test("its controller surveils 2 when the enchanted creature dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Colossal Dreadmaw")
                    .withCardAttachedTo(1, "Failed Conversion", "Colossal Dreadmaw")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Doom Blade", game.findPermanent("Colossal Dreadmaw")!!).error shouldBe null
                game.resolveStack()

                withClue("the surveil looks at two cards") {
                    val d = game.getPendingDecision()
                    (d is SelectCardsDecision) shouldBe true
                    d as SelectCardsDecision
                    d.options.size shouldBe 2
                    game.selectCards(d.options).error shouldBe null
                }
                game.resolveStack()

                game.isInGraveyard(2, "Colossal Dreadmaw") shouldBe true
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
