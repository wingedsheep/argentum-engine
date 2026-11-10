package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Skin Invasion // Skin Shedder (SOI #182).
 *
 *   Front — Enchant creature. Enchanted creature attacks each combat if able. When enchanted
 *           creature dies, return this card to the battlefield transformed under your control.
 *   Back  — Skin Shedder, 3/4 Insect Horror.
 *
 * Pins the ruling that matters for the usual play — the Aura on an opponent's creature: the
 * Aura's controller, not the dead creature's, gets Skin Shedder.
 */
class SkinInvasionScenarioTest : ScenarioTestBase() {

    init {
        context("Skin Invasion") {

            test("when the enchanted opponent's creature dies, Skin Shedder returns under the Aura controller's control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Skin Invasion", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the creature stays dead, the Aura left the graveyard") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Skin Invasion") shouldBe false
                }
                withClue("Skin Shedder is a 3/4 creature under Player1's control") {
                    val shedder = game.findPermanent("Skin Shedder").shouldNotBeNull()
                    val projected = game.state.projectedState
                    projected.getController(shedder) shouldBe game.player1Id
                    projected.isCreature(shedder) shouldBe true
                    projected.getPower(shedder) shouldBe 3
                    projected.getToughness(shedder) shouldBe 4
                }
            }
        }
    }
}
