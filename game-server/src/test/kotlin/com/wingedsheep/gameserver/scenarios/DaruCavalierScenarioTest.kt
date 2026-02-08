package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Daru Cavalier.
 *
 * Card reference:
 * - Daru Cavalier ({3}{W}): Creature — Human Soldier, 2/2
 *   First strike
 *   "When Daru Cavalier enters the battlefield, you may search your library
 *   for a card named Daru Cavalier, reveal it, put it into your hand, then shuffle."
 */
class DaruCavalierScenarioTest : ScenarioTestBase() {

    init {
        context("Daru Cavalier ETB search") {

            test("ETB triggers search for card named Daru Cavalier") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Daru Cavalier")
                    .withCardInLibrary(1, "Daru Cavalier") // Second copy in library
                    .withCardInLibrary(1, "Plains")        // Filler
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cast Daru Cavalier
                val castResult = game.castSpell(1, "Daru Cavalier")
                withClue("Casting Daru Cavalier should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell (creature enters battlefield)
                game.resolveStack()

                // ETB trigger — MayEffect asks yes/no
                withClue("Should have pending may decision for search") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose yes to search
                game.answerYesNo(true)

                // Should now have a search decision
                withClue("Should have pending search decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Find the Daru Cavalier in library to select
                val libraryCards = game.state.getLibrary(game.player1Id)
                val cavalierInLibrary = libraryCards.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Daru Cavalier"
                }

                withClue("Should find Daru Cavalier in library") {
                    (cavalierInLibrary != null) shouldBe true
                }

                // Select the card
                game.selectCards(listOf(cavalierInLibrary!!))

                // Verify the card went to hand
                withClue("Should have Daru Cavalier in hand after search") {
                    game.isInHand(1, "Daru Cavalier") shouldBe true
                }

                // Verify on battlefield
                withClue("First Daru Cavalier should be on battlefield") {
                    game.isOnBattlefield("Daru Cavalier") shouldBe true
                }
            }

            test("may decline to search") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Daru Cavalier")
                    .withCardInLibrary(1, "Daru Cavalier")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Daru Cavalier")
                game.resolveStack()

                // Decline the may effect
                game.answerYesNo(false)

                // Daru Cavalier should be on battlefield
                withClue("Daru Cavalier should be on battlefield") {
                    game.isOnBattlefield("Daru Cavalier") shouldBe true
                }

                // The copy should still be in library
                val libraryCards = game.state.getLibrary(game.player1Id)
                val cavalierInLibrary = libraryCards.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Daru Cavalier"
                }
                withClue("Daru Cavalier should still be in library after declining") {
                    (cavalierInLibrary != null) shouldBe true
                }
            }
        }

        context("Daru Cavalier first strike") {

            test("has first strike in combat") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Daru Cavalier") // 2/2 first strike
                    .withCardOnBattlefield(2, "Elvish Warrior") // 2/3 no first strike
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Daru Cavalier
                val attackResult = game.declareAttackers(mapOf("Daru Cavalier" to 2))
                withClue("Should be able to attack: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers and block with Elvish Warrior
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Elvish Warrior" to listOf("Daru Cavalier")))

                // Let combat resolve through first strike and regular damage
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Daru Cavalier (2/2 first strike) vs Elvish Warrior (2/3)
                // First strike: Daru deals 2 to Elvish Warrior (2/3 -> takes 2, survives with 1 toughness remaining)
                // Regular: Elvish Warrior deals 2 to Daru Cavalier (2/2 -> dies)
                withClue("Daru Cavalier should die from regular combat damage") {
                    game.isOnBattlefield("Daru Cavalier") shouldBe false
                }
                withClue("Elvish Warrior (2/3) should survive first strike damage of 2") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }
            }
        }
    }
}
