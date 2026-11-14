package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Exquisite Firecraft (ORI #143).
 *
 * "Exquisite Firecraft deals 4 damage to any target.
 *  Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 *  this spell can't be countered."
 *
 * Player 1 aims Firecraft at player 2, who answers with Cancel. Spell mastery is read off the
 * spell on the stack when Cancel resolves: only the caster's graveyard counts, only instant and
 * sorcery cards count, and Firecraft itself (still on the stack) never counts.
 */
class ExquisiteFirecraftScenarioTest : ScenarioTestBase() {

    private fun battle(
        casterGraveyard: List<String> = emptyList(),
        opponentGraveyard: List<String> = emptyList(),
    ): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Exquisite Firecraft")
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardInHand(2, "Cancel")
            .withLandsOnBattlefield(2, "Island", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        casterGraveyard.forEach { builder.withCardInGraveyard(1, it) }
        opponentGraveyard.forEach { builder.withCardInGraveyard(2, it) }
        return builder.build()
    }

    private fun TestGame.firecraftThenCancel() {
        castSpellTargetingPlayer(1, "Exquisite Firecraft", 2).error shouldBe null
        passPriority()
        withClue("Cancel can always target Firecraft — spell mastery only stops the counter") {
            castSpellTargetingStackSpell(2, "Cancel", "Exquisite Firecraft").error shouldBe null
        }
        resolveStack()
    }

    init {
        context("Exquisite Firecraft") {

            test("deals 4 damage to any target") {
                val game = battle()
                game.castSpellTargetingPlayer(1, "Exquisite Firecraft", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 16
                game.isInGraveyard(1, "Exquisite Firecraft") shouldBe true
            }

            test("spell mastery: with two instant/sorcery cards in your graveyard, Cancel can't counter it") {
                val game = battle(casterGraveyard = listOf("Lightning Bolt", "Divination"))
                game.firecraftThenCancel()

                withClue("Firecraft resolved despite Cancel") {
                    game.getLifeTotal(2) shouldBe 16
                    game.isInGraveyard(2, "Cancel") shouldBe true
                    game.isInGraveyard(1, "Exquisite Firecraft") shouldBe true
                }
            }

            test("one instant in your graveyard isn't enough — Firecraft on the stack doesn't count itself") {
                val game = battle(casterGraveyard = listOf("Lightning Bolt"))
                game.firecraftThenCancel()

                withClue("Firecraft was countered, so no damage") {
                    game.getLifeTotal(2) shouldBe 20
                    game.isInGraveyard(1, "Exquisite Firecraft") shouldBe true
                }
            }

            test("non-instant, non-sorcery cards don't count toward spell mastery") {
                val game = battle(casterGraveyard = listOf("Craw Wurm", "Mountain"))
                game.firecraftThenCancel()

                game.getLifeTotal(2) shouldBe 20
            }

            test("instants and sorceries in the opponent's graveyard don't count") {
                val game = battle(opponentGraveyard = listOf("Lightning Bolt", "Divination"))
                game.firecraftThenCancel()

                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
