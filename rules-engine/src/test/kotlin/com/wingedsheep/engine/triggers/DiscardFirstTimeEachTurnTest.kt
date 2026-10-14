package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe

/**
 * `EventPattern.DiscardEvent.firstTimeEachTurn` — "whenever you discard one or more nonland cards
 * for the first time each turn". Detection reads the post-resolution discard history, so two
 * discard events in one resolution must still tell which came first: the event's
 * `priorDiscardsThisTurn` stamp marks off the history that precedes it.
 */
class DiscardFirstTimeEachTurnTest : ScenarioTestBase() {
    init {
        val watcher = card("First Discard Test Watcher") {
            typeLine = "Artifact"
            triggeredAbility {
                trigger = Triggers.you.discards(GameObjectFilter.Nonland, batch = true, firstTimeEachTurn = true)
                effect = Effects.GainLife(1)
            }
        }
        val nonlandTwice = card("First Discard Test Nonland Twice") {
            manaCost = "{R}"
            typeLine = "Sorcery"
            spell {
                effect = Patterns.Hand.discardCards(1, filter = GameObjectFilter.Nonland) then
                    Patterns.Hand.discardCards(1, filter = GameObjectFilter.Nonland)
            }
        }
        val landThenNonland = card("First Discard Test Land Then Nonland") {
            manaCost = "{R}"
            typeLine = "Sorcery"
            spell {
                effect = Patterns.Hand.discardCards(1, filter = GameObjectFilter.Land) then
                    Patterns.Hand.discardCards(1, filter = GameObjectFilter.Nonland)
            }
        }
        listOf(watcher, nonlandTwice, landThenNonland).forEach(cardRegistry::register)

        fun cast(spell: String, hand: List<String>, discards: List<String>): TestGame {
            val game = scenario()
                .withPlayers("First", "Second")
                .withCardOnBattlefield(1, watcher.name)
                .withCardInHand(1, spell)
                .apply { hand.forEach { withCardInHand(1, it) } }
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, spell).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            for (name in discards) {
                if (game.hasPendingDecision()) game.selectCards(listOf(game.findCardsInHand(1, name).first()))
                game.resolveStack()
            }
            return game
        }

        test("two nonland discard events in one resolution fire only for the first") {
            val game = cast(nonlandTwice.name, listOf("Grizzly Bears", "Grizzly Bears"), listOf("Grizzly Bears", "Grizzly Bears"))
            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
            game.getLifeTotal(1) shouldBe 21
        }

        test("a land discard first leaves the window open for the nonland discard after it") {
            val game = cast(landThenNonland.name, listOf("Forest", "Grizzly Bears"), listOf("Forest", "Grizzly Bears"))
            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
            game.getLifeTotal(1) shouldBe 21
        }

        test("firstTimeEachTurn needs the batch wording") {
            shouldThrow<IllegalArgumentException> {
                EventPattern.DiscardEvent(cardFilter = GameObjectFilter.Nonland, firstTimeEachTurn = true)
            }
        }
    }
}
