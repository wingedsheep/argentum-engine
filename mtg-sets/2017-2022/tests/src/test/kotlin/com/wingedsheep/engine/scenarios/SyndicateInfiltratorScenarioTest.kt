package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Syndicate Infiltrator (SNC #226) — {2}{U}{B} Creature — Vampire Wizard, 3/3.
 *
 * "Flying
 *  As long as there are five or more mana values among cards in your graveyard, this creature gets +2/+2."
 *
 * Covers that the gate counts *distinct* mana values (duplicates count once, a land is mana value 0),
 * that only your own graveyard counts, and that the bonus turns on once a fifth value arrives.
 */
class SyndicateInfiltratorScenarioTest : ScenarioTestBase() {

    private fun sorcery(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Sorcery"
    }

    init {
        cardRegistry.register(sorcery("Test One", "{B}"))
        cardRegistry.register(sorcery("Test One Again", "{U}"))
        cardRegistry.register(sorcery("Test Two", "{1}{B}"))
        cardRegistry.register(sorcery("Test Three", "{2}{B}"))
        cardRegistry.register(sorcery("Test Four", "{3}{B}"))

        context("Syndicate Infiltrator") {

            test("four distinct mana values (with duplicates) leave it a 3/3; a fifth value makes it 5/5") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Syndicate Infiltrator")
                    .withCardInGraveyard(1, "Swamp")          // 0
                    .withCardInGraveyard(1, "Test One")       // 1
                    .withCardInGraveyard(1, "Test One Again") // 1 again
                    .withCardInGraveyard(1, "Test Two")       // 2
                    .withCardInGraveyard(1, "Test Three")     // 3
                    .withCardInGraveyard(2, "Test Four")      // opponent's 4 doesn't count
                    .withCardInHand(1, "Test Four")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val infiltrator = game.findPermanent("Syndicate Infiltrator")!!
                withClue("values 0, 1, 2, 3 in your graveyard are only four") {
                    game.state.projectedState.getPower(infiltrator) shouldBe 3
                    game.state.projectedState.getToughness(infiltrator) shouldBe 3
                }

                game.castSpell(1, "Test Four").error shouldBe null
                game.resolveStack()

                withClue("the resolved {3}{B} sorcery adds mana value 4 — five values") {
                    game.state.projectedState.getPower(infiltrator) shouldBe 5
                    game.state.projectedState.getToughness(infiltrator) shouldBe 5
                }
            }
        }
    }
}
