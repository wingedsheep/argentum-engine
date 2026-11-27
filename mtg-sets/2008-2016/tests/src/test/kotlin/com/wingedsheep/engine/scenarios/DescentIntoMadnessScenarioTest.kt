package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Descent into Madness (AVR #97; {3}{B}{B} Enchantment).
 *
 *   At the beginning of your upkeep, put a despair counter on this enchantment, then each player
 *   exiles X permanents they control and/or cards from their hand, where X is the number of despair
 *   counters on this enchantment.
 *
 * Covers the despair tally sizing the exile, the active player choosing first, a player with fewer
 * than X cards exiling everything they have, and the last-known count when the enchantment is gone.
 */
class DescentIntoMadnessScenarioTest : ScenarioTestBase() {

    private fun TestGame.despair(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.DESPAIR) ?: 0

    private fun TestGame.setDespair(id: EntityId, n: Int) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent().withAdded(CounterType.DESPAIR, n)) }
    }

    init {
        context("Descent into Madness") {

            test("adds a despair counter, then each player exiles that many — active player first") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Descent into Madness")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(2, "Giant Growth")
                    .withCardInHand(2, "Shock")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()
                val descent = game.findPermanent("Descent into Madness")!!
                game.setDespair(descent, 1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                val mine = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected the active player's exile choice; got ${game.getPendingDecision()}")
                mine.playerId shouldBe game.player1Id
                withClue("X is the despair count after this upkeep's counter") { game.despair(descent) shouldBe 2 }
                val bears = game.findPermanent("Grizzly Bears")!!
                val bolt = game.findCardsInHand(1, "Lightning Bolt").single()
                game.selectCards(listOf(bears, bolt)).error shouldBe null

                val theirs = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected the opponent's exile choice; got ${game.getPendingDecision()}")
                theirs.playerId shouldBe game.player2Id
                val giant = game.findPermanent("Hill Giant")!!
                val shock = game.findCardsInHand(2, "Shock").single()
                game.selectCards(listOf(giant, shock)).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(1, "Lightning Bolt") shouldBe true
                game.isInExile(2, "Hill Giant") shouldBe true
                game.isInExile(2, "Shock") shouldBe true
                game.isInHand(2, "Giant Growth") shouldBe true
                game.isOnBattlefield("Descent into Madness") shouldBe true
            }

            test("a player with fewer than X cards in hand and permanents exiles all of them") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Descent into Madness")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()
                val descent = game.findPermanent("Descent into Madness")!!
                game.setDespair(descent, 2)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // X = 3: the active player has exactly Descent plus three cards in hand.
                val bolt = game.findCardsInHand(1, "Lightning Bolt").single()
                val shock = game.findCardsInHand(1, "Shock").single()
                val growth = game.findCardsInHand(1, "Giant Growth").single()
                game.selectCards(listOf(bolt, shock, growth)).error shouldBe null
                // The opponent's only card is Hill Giant: fewer than three, so it goes without a choice.
                (game.getPendingDecision() as? SelectCardsDecision)?.let {
                    it.playerId shouldBe game.player2Id
                    game.selectCards(it.options).error shouldBe null
                }
                game.resolveStack()

                game.isInExile(2, "Hill Giant") shouldBe true
                game.isInExile(1, "Lightning Bolt") shouldBe true
                game.isOnBattlefield("Descent into Madness") shouldBe true
            }

            test("destroyed in response, it uses the despair count it had when it left") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Descent into Madness")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInHand(2, "Naturalize")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()
                val descent = game.findPermanent("Descent into Madness")!!
                game.setDespair(descent, 1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passPriority() // the active player passes with the trigger on the stack
                game.castSpell(2, "Naturalize", descent).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Descent into Madness") shouldBe true

                // X = 1 (its last-known count; this upkeep's counter never landed).
                val mine = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected the active player's exile choice; got ${game.getPendingDecision()}")
                mine.playerId shouldBe game.player1Id
                game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
                val theirs = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected the opponent's exile choice; got ${game.getPendingDecision()}")
                game.selectCards(listOf(theirs.options.first())).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.findPermanents("Forest").size shouldBe 1
            }
        }
    }
}
