package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Fiery Impulse (ORI #145).
 *
 * "Fiery Impulse deals 2 damage to target creature.
 *  Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 *  Fiery Impulse deals 3 damage instead."
 *
 * Aimed at a 3/3 Hill Giant: 2 damage leaves it alive, 3 kills it. Spell mastery is checked as
 * Fiery Impulse resolves: only the caster's graveyard counts, only instant and sorcery cards
 * count, and Fiery Impulse itself (still on the stack) never counts.
 */
class FieryImpulseScenarioTest : ScenarioTestBase() {

    private fun battle(
        casterGraveyard: List<String> = emptyList(),
        opponentGraveyard: List<String> = emptyList(),
        extraInHand: List<String> = emptyList(),
    ): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Fiery Impulse")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        casterGraveyard.forEach { builder.withCardInGraveyard(1, it) }
        opponentGraveyard.forEach { builder.withCardInGraveyard(2, it) }
        extraInHand.forEach { builder.withCardInHand(1, it) }
        return builder.build()
    }

    private fun TestGame.impulseTheGiant() {
        val giant = findPermanent("Hill Giant")!!
        castSpell(1, "Fiery Impulse", giant).error shouldBe null
    }

    init {
        context("Fiery Impulse") {

            test("deals 2 damage to target creature without spell mastery") {
                val game = battle()
                game.impulseTheGiant()
                game.resolveStack()

                withClue("Hill Giant (3/3) survives 2 damage") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                game.isInGraveyard(1, "Fiery Impulse") shouldBe true
            }

            test("spell mastery: two instant/sorcery cards in your graveyard make it deal 3") {
                val game = battle(casterGraveyard = listOf("Lightning Bolt", "Divination"))
                game.impulseTheGiant()
                game.resolveStack()

                withClue("3 damage kills Hill Giant") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }

            test("one instant in your graveyard isn't enough — Fiery Impulse doesn't count itself") {
                val game = battle(casterGraveyard = listOf("Lightning Bolt"))
                game.impulseTheGiant()
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("non-instant, non-sorcery cards don't count") {
                val game = battle(casterGraveyard = listOf("Grizzly Bears", "Mountain"))
                game.impulseTheGiant()
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("instants and sorceries in the opponent's graveyard don't count") {
                val game = battle(opponentGraveyard = listOf("Lightning Bolt", "Divination"))
                game.impulseTheGiant()
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("spell mastery is checked on resolution, not on cast") {
                // One sorcery in the graveyard when Fiery Impulse is cast; a Lightning Bolt cast in
                // response resolves first and becomes the second instant/sorcery card.
                val game = battle(casterGraveyard = listOf("Divination"), extraInHand = listOf("Lightning Bolt"))
                game.impulseTheGiant()
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                withClue("Bolt reached the graveyard before Impulse resolved, so it dealt 3") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
            }
        }
    }
}
