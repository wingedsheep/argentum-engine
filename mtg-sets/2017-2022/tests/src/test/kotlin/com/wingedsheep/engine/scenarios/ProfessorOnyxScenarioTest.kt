package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.stx.cards.ProfessorOnyx
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Professor Onyx (STX #83) — {4}{B}{B} Legendary Planeswalker — Liliana, loyalty 5.
 *
 * Covers the two compositions a reader can't confirm from the script alone: the magecraft
 * loss-then-gain, and the −8's seven-fold punisher (`Effects.Repeat` over a per-opponent
 * `Effects.PayOrSuffer` discard), where each round is a fresh choice and an empty hand loses 3
 * without being asked.
 */
class ProfessorOnyxScenarioTest : ScenarioTestBase() {

    private val minusEight = ProfessorOnyx.activatedAbilities[2].id

    init {
        context("Professor Onyx") {

            test("magecraft: casting an instant makes each opponent lose 2 and you gain 2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Professor Onyx")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                withClue("Bolt's 3 plus magecraft's 2") { game.getLifeTotal(2) shouldBe 15 }
                withClue("magecraft gains a fixed 2") { game.getLifeTotal(1) shouldBe 22 }
            }

            test("−8: seven rounds of discard-or-lose-3, each a fresh choice") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Professor Onyx")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val onyx = game.findPermanent("Professor Onyx")!!
                game.state = game.state.updateEntity(onyx) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 8))
                }

                game.execute(ActivateAbility(game.player1Id, onyx, minusEight)).error shouldBe null
                game.resolveStack()

                // Rounds 1 and 2: discard. Round 3: decline with a card still in hand (lose 3).
                // Round 4: discard the last card. Rounds 5–7: the empty hand loses 3 each time
                // without a decision.
                var round = 0
                var guard = 0
                while (game.hasPendingDecision() && guard++ < 20) {
                    round++
                    val hand = game.state.getHand(game.player2Id)
                    if (round == 3) {
                        game.skipSelection()
                    } else {
                        game.selectCards(listOf(hand.first()))
                    }
                    game.resolveStack()
                }

                withClue("three discards were offered and taken, one declined") {
                    round shouldBe 4
                    game.handSize(2) shouldBe 0
                }
                withClue("lost 3 life in round 3 and rounds 5–7: 4 × 3 = 12") {
                    game.getLifeTotal(2) shouldBe 8
                }
            }
        }
    }
}
