package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Welcome the Dead (TDC #30) — draw two, discard one, lose 2 life, then create X tapped 2/2 Zombie
 * Druids, where X is the number of cards put into your graveyard from your hand or library this turn.
 */
class WelcomeTheDeadScenarioTest : ScenarioTestBase() {

    init {
        fun game(): TestGame {
            var b = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Welcome the Dead")
                .withCardInHand(1, "Tome Scour")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(10) { b = b.withCardInLibrary(1, "Swamp") }
            repeat(10) { b = b.withCardInLibrary(2, "Swamp") }
            return b.build()
        }

        fun TestGame.castWelcomeDiscarding(cardName: String) {
            castSpell(1, "Welcome the Dead").error shouldBe null
            resolveStack()
            withClue("the spell asks which card to discard") { hasPendingDecision() shouldBe true }
            selectCards(listOf(findCardsInHand(1, cardName).first())).error shouldBe null
        }

        fun TestGame.zombieDruids() = findPermanents("Zombie Druid")

        context("Welcome the Dead") {
            test("its own discard counts: one tapped Zombie Druid, and you lose 2 life") {
                val game = game()
                game.castWelcomeDiscarding("Tome Scour")

                game.isInGraveyard(1, "Tome Scour") shouldBe true
                game.getLifeTotal(1) shouldBe 18
                withClue("only the discarded card went to your graveyard from hand or library") {
                    game.zombieDruids().size shouldBe 1
                }
                game.zombieDruids().all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
            }

            test("cards milled from your library earlier this turn count too") {
                val game = game()
                game.castSpellTargetingPlayer(1, "Tome Scour", 1).error shouldBe null
                game.resolveStack()

                game.castWelcomeDiscarding("Swamp")

                withClue("five milled + one discarded; Tome Scour went there from the stack and doesn't count") {
                    game.zombieDruids().size shouldBe 6
                }
            }

            test("an opponent's milled cards don't count") {
                val game = game()
                game.castSpellTargetingPlayer(1, "Tome Scour", 2).error shouldBe null
                game.resolveStack()

                game.castWelcomeDiscarding("Swamp")

                game.zombieDruids().size shouldBe 1
            }
        }
    }
}
