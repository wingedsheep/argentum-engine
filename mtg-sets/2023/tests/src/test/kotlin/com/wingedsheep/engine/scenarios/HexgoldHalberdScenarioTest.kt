package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hexgold Halberd (ONE #136) — For Mirrodin! makes a 2/2 red Rebel and attaches to it; during its
 * controller's turn the equipped creature has first strike and trample, and not otherwise.
 */
class HexgoldHalberdScenarioTest : ScenarioTestBase() {

    init {
        context("Hexgold Halberd") {
            test("For Mirrodin! Rebel has first strike and trample during your turn") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Hexgold Halberd")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Hexgold Halberd")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val rebel = game.findPermanent("Rebel Token")!!
                val halberd = game.findPermanent("Hexgold Halberd")!!
                game.state.getEntity(halberd)?.get<AttachedToComponent>()?.targetId shouldBe rebel

                val projected = game.state.projectedState
                projected.hasKeyword(rebel, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(rebel, Keyword.TRAMPLE) shouldBe true
                projected.getPower(rebel) shouldBe 2
                projected.getToughness(rebel) shouldBe 2
            }

            test("equipped creature lacks first strike and trample during an opponent's turn") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Hexgold Halberd", "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                withClue("Not Alice's turn: no keywords granted") {
                    projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
                    projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("an unequipped creature gains nothing during your turn") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hexgold Halberd")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
            }
        }
    }
}
