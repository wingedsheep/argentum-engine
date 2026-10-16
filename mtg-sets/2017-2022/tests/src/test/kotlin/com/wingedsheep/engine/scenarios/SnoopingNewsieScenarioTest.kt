package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Snooping Newsie (SNC #222) — {U}{B} Creature — Human Rogue, 2/2.
 *
 * "When this creature enters, mill two cards.
 *  As long as there are five or more mana values among cards in your graveyard, this creature gets
 *  +1/+1 and has lifelink."
 *
 * Covers the enters-mill feeding its own gate: three values before, and the two milled cards bring
 * five only when both add new values — then both halves of the static switch on together.
 */
class SnoopingNewsieScenarioTest : ScenarioTestBase() {

    private fun sorcery(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Sorcery"
    }

    init {
        cardRegistry.register(sorcery("Test One", "{B}"))
        cardRegistry.register(sorcery("Test Two", "{1}{B}"))
        cardRegistry.register(sorcery("Test Three", "{2}{B}"))
        cardRegistry.register(sorcery("Test Four", "{3}{B}"))
        cardRegistry.register(sorcery("Test Five", "{4}{B}"))

        context("Snooping Newsie") {

            fun newsieAfterMilling(top: String, second: String) = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Snooping Newsie")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInGraveyard(1, "Test One")
                .withCardInGraveyard(1, "Test Two")
                .withCardInGraveyard(1, "Test Three")
                .withCardInLibrary(1, top)
                .withCardInLibrary(1, second)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
                .also { game ->
                    game.castSpell(1, "Snooping Newsie").error shouldBe null
                    game.resolveStack() // the creature spell, then its enters trigger
                }

            test("milling two new mana values makes five: 3/3 with lifelink") {
                val game = newsieAfterMilling("Test Four", "Test Five")
                val newsie = game.findPermanent("Snooping Newsie")!!
                withClue("values 1, 2, 3 plus the milled 4 and 5") {
                    game.state.projectedState.getPower(newsie) shouldBe 3
                    game.state.projectedState.getToughness(newsie) shouldBe 3
                    game.state.projectedState.hasKeyword(newsie, Keyword.LIFELINK) shouldBe true
                }
            }

            test("milling duplicate mana values leaves four: a plain 2/2") {
                val game = newsieAfterMilling("Test Four", "Test One")
                val newsie = game.findPermanent("Snooping Newsie")!!
                withClue("the second milled card repeats mana value 1 — only 1, 2, 3, 4") {
                    game.state.projectedState.getPower(newsie) shouldBe 2
                    game.state.projectedState.getToughness(newsie) shouldBe 2
                    game.state.projectedState.hasKeyword(newsie, Keyword.LIFELINK) shouldBe false
                }
            }
        }
    }
}
