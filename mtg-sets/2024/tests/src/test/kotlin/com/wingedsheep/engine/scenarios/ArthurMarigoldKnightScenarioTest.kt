package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Arthur, Marigold Knight (BLC #5): "Whenever Arthur and at least one other creature attack, look
 * at the top six cards of your library. You may put a creature card from among them onto the
 * battlefield tapped and attacking. Put the rest on the bottom of your library in a random order.
 * Return that creature to its owner's hand at end of combat."
 *
 * Proves the composition the snapshot can't: the creature tracked out of the look pipeline enters
 * tapped and attacking, and the per-creature delayed trigger scheduled during declare attackers
 * returns it to hand at this combat's end-of-combat step.
 */
class ArthurMarigoldKnightScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Arthur Test Squire",
                manaCost = ManaCost.parse("{1}{W}"),
                subtypes = setOf(Subtype("Mouse")),
                power = 1,
                toughness = 1
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Arthur Test Recruit",
                manaCost = ManaCost.parse("{2}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 3,
                toughness = 3
            )
        )

        context("Arthur, Marigold Knight") {

            test("with another attacker, puts a creature from the top six in attacking, returned at end of combat") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Arthur, Marigold Knight", summoningSickness = false)
                    .withCardOnBattlefield(1, "Arthur Test Squire", summoningSickness = false)
                    .withCardInLibrary(1, "Arthur Test Recruit")
                repeat(5) { builder.withCardInLibrary(1, "Island") }
                val game = builder
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Arthur, Marigold Knight" to 2, "Arthur Test Squire" to 2)
                ).error shouldBe null
                game.resolveStack()

                withClue("the trigger pauses to choose a creature from the top six") {
                    game.hasPendingDecision() shouldBe true
                }
                val recruit = game.findCardsInLibrary(1, "Arthur Test Recruit").single()
                game.selectCards(listOf(recruit)).error shouldBe null
                game.resolveStack()

                val entered = game.findPermanents("Arthur Test Recruit").single()
                withClue("it enters tapped and attacking") {
                    game.state.getEntity(entered)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(entered)?.has<AttackingComponent>() shouldBe true
                }
                withClue("the five other cards went back to the library") {
                    game.librarySize(1) shouldBe 5
                }

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("the creature returns to its owner's hand at end of combat") {
                    game.findPermanents("Arthur Test Recruit").size shouldBe 0
                    game.isInHand(1, "Arthur Test Recruit") shouldBe true
                }
            }

            test("attacking alone does not trigger") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Arthur, Marigold Knight", summoningSickness = false)
                    .withCardInLibrary(1, "Arthur Test Recruit")
                repeat(5) { builder.withCardInLibrary(1, "Island") }
                val game = builder
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Arthur, Marigold Knight" to 2)).error shouldBe null
                game.resolveStack()

                withClue("no look, no choice") {
                    game.hasPendingDecision() shouldBe false
                    game.findPermanents("Arthur Test Recruit").size shouldBe 0
                    game.librarySize(1) shouldBe 6
                }
            }
        }
    }
}
