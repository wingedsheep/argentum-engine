package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.matchers.shouldBe

/**
 * `Triggers.you.discards(batch = true)` — "whenever you discard one or more cards" carries the
 * discarded cards that match its card filter as the trigger's captured collection, so the payoff
 * can act on "them" (Toluz, Clever Conductor: "exile them from your graveyard").
 */
class DiscardBatchCapturedCardsTest : ScenarioTestBase() {
    init {
        fun exileThem(name: String, filter: GameObjectFilter?) = card(name) {
            typeLine = "Artifact"
            triggeredAbility {
                trigger = Triggers.you.discards(filter, batch = true)
                effect = Effects.Pipeline {
                    exile(filter(triggerCaptured, GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD)))
                }
            }
        }
        val anyWatcher = exileThem("Batch Discard Test Any Watcher", null)
        val nonlandWatcher = exileThem("Batch Discard Test Nonland Watcher", GameObjectFilter.Nonland)
        val discardTwo = card("Batch Discard Test Discard Two") {
            manaCost = "{R}"
            typeLine = "Sorcery"
            spell { effect = Patterns.Hand.discardCards(2) }
        }
        listOf(anyWatcher, nonlandWatcher, discardTwo).forEach(cardRegistry::register)

        fun discardBearsAndForest(watcher: String): TestGame {
            val game = scenario()
                .withPlayers("First", "Second")
                .withCardOnBattlefield(1, watcher)
                .withCardInHand(1, discardTwo.name)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Forest")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, discardTwo.name).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears") + game.findCardsInHand(1, "Forest"))
            }
            game.resolveStack()
            return game
        }

        test("one firing captures every card the discard put into the graveyard") {
            val game = discardBearsAndForest(anyWatcher.name)
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInExile(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }

        test("the captured collection holds only the cards matching the trigger's card filter") {
            val game = discardBearsAndForest(nonlandWatcher.name)
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Forest") shouldBe true
        }
    }
}
