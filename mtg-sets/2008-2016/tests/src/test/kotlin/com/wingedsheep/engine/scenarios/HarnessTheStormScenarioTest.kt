package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Harness the Storm (SOI #163) — {2}{R} Enchantment.
 *
 *   Whenever you cast an instant or sorcery spell from your hand, you may cast target card with
 *   the same name as that spell from your graveyard. (You still pay its costs.)
 *
 * Pins the target filter that reads the *triggering spell's* name as the trigger goes on the
 * stack, the resolution-time cast from the graveyard paying its normal cost (the card goes back to
 * the graveyard, no exile rider), and that the graveyard cast — not from the hand — doesn't
 * trigger it again.
 */
class HarnessTheStormScenarioTest : ScenarioTestBase() {

    private fun harnessGame(vararg graveyard: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Harness the Storm")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .let { builder -> graveyard.fold(builder) { b, name -> b.withCardInGraveyard(1, name) } }
        .withCardInGraveyard(2, "Lightning Bolt") // the opponent's graveyard doesn't count
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Harness the Storm") {

            test("only cards in your graveyard with the cast spell's name can be targeted") {
                val game = harnessGame("Lightning Bolt", "Lightning Bolt", "Shock")

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()

                withClue("your two Lightning Bolts are the legal targets; Shock and the opponent's Bolt aren't") {
                    decision.legalTargets.getValue(0).toSet() shouldBe
                        game.findCardsInGraveyard(1, "Lightning Bolt").toSet()
                }
            }

            test("casting the graveyard copy pays its cost, and it returns to the graveyard as it resolves") {
                val game = harnessGame("Lightning Bolt", "Lightning Bolt")

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.selectTargets(listOf(game.findCardsInGraveyard(1, "Lightning Bolt").first())).error shouldBe null
                game.resolveStack()

                game.answerYesNo(true).error shouldBe null
                // The graveyard Bolt's own target, chosen as it's cast.
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("both Bolts resolved against the opponent") {
                    game.getLifeTotal(2) shouldBe 14
                }
                withClue("the graveyard cast paid {R}: both Mountains are tapped") {
                    game.findPermanents("Mountain").all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
                withClue("casting from the graveyard isn't casting from the hand — no second trigger") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.size shouldBe 0
                }
                withClue("both cast Bolts went to the graveyard, not exile") {
                    game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 3
                    game.isInExile(1, "Lightning Bolt") shouldBe false
                }
            }

            test("declining leaves the card in the graveyard") {
                val game = harnessGame("Lightning Bolt")

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                if (game.getPendingDecision() is ChooseTargetsDecision) {
                    game.selectTargets(game.findCardsInGraveyard(1, "Lightning Bolt")).error shouldBe null
                }
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                withClue("the declined Bolt stays, and the hand Bolt joins it") {
                    game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 2
                }
                withClue("nothing was paid for a cast that didn't happen") {
                    game.findPermanents("Mountain").count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 1
                }
            }

            test("with no card of that name in your graveyard, nothing happens") {
                val game = harnessGame("Shock")

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                withClue("the trigger has no legal target, so there's nothing to choose") {
                    game.hasPendingDecision() shouldBe false
                }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.findCardsInGraveyard(1, "Shock").size shouldBe 1
            }
        }
    }
}
