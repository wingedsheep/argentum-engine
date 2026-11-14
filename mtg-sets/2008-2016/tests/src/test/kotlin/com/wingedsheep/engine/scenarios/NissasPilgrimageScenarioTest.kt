package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nissa's Pilgrimage — {2}{G} Sorcery
 * "Search your library for up to two basic Forest cards, reveal those cards, and put one onto the
 * battlefield tapped and the rest into your hand. Then shuffle.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, search
 * your library for up to three basic Forest cards instead of two."
 *
 * Proves the spell-mastery count reaches the search cap at resolution, that only basic Forests are
 * searchable, and that with three found exactly one enters tapped and the other two go to hand.
 */
class NissasPilgrimageScenarioTest : ScenarioTestBase() {

    private fun game(graveyard: List<String>) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Nissa's Pilgrimage")
        .withLandsOnBattlefield(1, "Forest", 3)
        .apply { graveyard.forEach { withCardInGraveyard(1, it) } }
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Plains")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("with spell mastery, up to three Forests: one enters tapped, two go to hand") {
            val g = game(listOf("Cultivate", "Lightning Bolt"))

            g.castSpell(1, "Nissa's Pilgrimage").error shouldBe null
            g.resolveStack()

            val forests = g.findCardsInLibrary(1, "Forest")
            val search = g.getPendingDecision() as SelectCardsDecision
            withClue("spell mastery raises the cap to three") { search.maxSelections shouldBe 3 }
            withClue("only basic Forests are searchable") {
                search.options.contains(g.findCardsInLibrary(1, "Plains").first()) shouldBe false
            }
            g.selectCards(forests).error shouldBe null

            (g.getPendingDecision() is SelectCardsDecision) shouldBe true
            g.selectCards(listOf(forests[0])).error shouldBe null
            g.resolveStack()

            withClue("the chosen Forest entered the battlefield tapped") {
                g.findPermanents("Forest").contains(forests[0]) shouldBe true
                (g.state.getEntity(forests[0])?.has<TappedComponent>() ?: false) shouldBe true
            }
            withClue("the other two Forests went to hand") {
                g.findCardsInHand(1, "Forest").toSet() shouldBe setOf(forests[1], forests[2])
            }
        }

        test("without spell mastery the search stops at two") {
            val g = game(listOf("Cultivate"))

            g.castSpell(1, "Nissa's Pilgrimage").error shouldBe null
            g.resolveStack()

            val search = g.getPendingDecision() as SelectCardsDecision
            search.maxSelections shouldBe 2
        }
    }
}
