package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.drk.cards.WandOfIth
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Wand of Ith.
 *
 * The scenario builder starts both hands empty, so giving the victim exactly one card makes the
 * "at random" reveal deterministic and pins which branch runs — the two branches charge different
 * prices and only one may fire per activation.
 *
 * The nonland branch is the one that needed new vocabulary: the price is the *revealed card's* mana
 * value, not a number printed on the Wand. Craw Wurm ({4}{G}{G}) costs 6 life to keep, so a
 * fixed-price implementation would charge 1 there and still pass a land-only test.
 */
class WandOfIthScenarioTest : ScenarioTestBase() {

    private val abilityId = WandOfIth.script.activatedAbilities.first().id

    init {
        context("Wand of Ith") {

            fun scenarioWith(cardInVictimsHand: String) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Wand of Ith")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInHand(2, cardInVictimsHand)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.fireAt(answerYes: Boolean) {
                val wand = findPermanent("Wand of Ith")!!
                execute(
                    ActivateAbility(
                        playerId = player1Id,
                        sourceId = wand,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(player2Id)),
                    )
                ).error shouldBe null
                var guard = 0
                while (guard++ < 12 && (state.stack.isNotEmpty() || hasPendingDecision())) {
                    if (hasPendingDecision()) answerYesNo(answerYes) else passPriority()
                }
            }

            test("a revealed nonland costs life equal to its mana value") {
                val game = scenarioWith("Craw Wurm")   // {4}{G}{G} — mana value 6
                game.fireAt(answerYes = true)

                withClue("paying keeps the card and costs its mana value, not a flat 1") {
                    game.getLifeTotal(2) shouldBe 14
                    game.handSize(2) shouldBe 1
                }
            }

            test("declining the nonland price discards it instead") {
                val game = scenarioWith("Craw Wurm")
                game.fireAt(answerYes = false)

                withClue("no life paid, and the card is gone") {
                    game.getLifeTotal(2) shouldBe 20
                    game.handSize(2) shouldBe 0
                    game.isInGraveyard(2, "Craw Wurm") shouldBe true
                }
            }

            test("a revealed land costs only 1 life") {
                val game = scenarioWith("Mountain")
                game.fireAt(answerYes = true)

                withClue("the land branch is a flat 1 life, whatever the card would otherwise cost") {
                    game.getLifeTotal(2) shouldBe 19
                    game.handSize(2) shouldBe 1
                }
            }
        }
    }
}
