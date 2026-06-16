package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Heated Argument (SOS #118).
 *
 * "{4}{R} Instant. Heated Argument deals 6 damage to target creature. You may exile a card from
 *  your graveyard. If you do, Heated Argument also deals 2 damage to that creature's controller."
 *
 * Verifies the 6 damage to the targeted creature always happens; the 2 damage to that creature's
 * controller only happens when the caster actually exiles a graveyard card (MayEffect + IfYouDo).
 */
class HeatedArgumentScenarioTest : ScenarioTestBase() {

    init {
        context("Heated Argument") {

            test("exiling a graveyard card deals 2 extra damage to the creature's controller") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Heated Argument")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInGraveyard(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Heated Argument", bears)
                game.resolveStack()

                // MayEffect: accept and exile the graveyard card.
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    val gy = game.state.getGraveyard(game.player1Id)
                    game.selectCards(gy.take(1))
                    game.resolveStack()
                }

                // 6 damage destroys the 2/2 Grizzly Bears.
                withClue("Grizzly Bears should be destroyed by 6 damage") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }

                withClue("Victim should take 2 damage to face (20 -> 18)") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("declining the exile deals no extra damage") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Heated Argument")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInGraveyard(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Heated Argument", bears)
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Grizzly Bears should still be destroyed by 6 damage") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("Victim should be untouched when the exile is declined") {
                    game.getLifeTotal(2) shouldBe 20
                }
                withClue("The Mountain should remain in the graveyard (not exiled)") {
                    val names = game.state.getGraveyard(game.player1Id).mapNotNull {
                        game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
                    }
                    names.contains("Mountain") shouldBe true
                }
            }
        }
    }
}
