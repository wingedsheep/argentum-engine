package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for the interaction between True Believer and Words of Waste.
 *
 * Card references:
 * - True Believer ({W}{W}): Creature — Human Cleric, 2/2
 *   "You have shroud. (You can't be the target of spells or abilities.)"
 * - Words of Waste ({2}{B}): Enchantment
 *   "{1}: The next time you would draw a card this turn, each opponent discards a card instead."
 *
 * Key ruling: Words of Waste does NOT target. It says "each opponent discards" —
 * no targeting word is used. Shroud only prevents targeting, so True Believer
 * does not protect its controller from the discard replacement effect.
 */
class TrueBelieverWordsOfWasteScenarioTest : ScenarioTestBase() {

    init {
        context("True Believer vs Words of Waste interaction") {
            test("Words of Waste still forces discard through shroud (does not target)") {
                // Setup:
                // - Player 1 (active) controls Words of Waste and has mana for activation + draw spell
                // - Player 2 controls True Believer and has cards in hand
                // - Player 1 activates Words of Waste, then casts Touch of Brilliance (draw 2)
                // - First draw should be replaced: Player 2 must discard despite having shroud
                val game = scenario()
                    .withPlayers("Words Player", "Shroud Player")
                    .withCardOnBattlefield(1, "Words of Waste")
                    .withCardInHand(1, "Touch of Brilliance")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "True Believer")
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialP2HandSize = game.handSize(2)

                // Activate Words of Waste
                val wordsId = game.findPermanent("Words of Waste")!!
                val wordsDef = cardRegistry.getCard("Words of Waste")!!
                val ability = wordsDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wordsId,
                        abilityId = ability.id
                    )
                )
                withClue("Words of Waste activation should succeed") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability (creates the replacement shield)
                game.resolveStack()

                // Cast Touch of Brilliance (draw 2)
                val castResult = game.castSpell(1, "Touch of Brilliance")
                withClue("Touch of Brilliance should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // First draw was replaced — Player 2 must choose a card to discard
                // despite having shroud from True Believer (Words of Waste doesn't target)
                withClue("Player 2 should have a pending discard decision (shroud doesn't help)") {
                    game.hasPendingDecision() shouldBe true
                    (game.getPendingDecision() is SelectCardsDecision) shouldBe true
                }

                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("Discard decision should be for Player 2") {
                    decision.playerId shouldBe game.player2Id
                }

                // Player 2 discards a card
                game.selectCards(listOf(decision.options.first()))

                // Player 2 lost 1 card to discard (shroud did not protect them)
                withClue("Player 2 should have lost 1 card from Words of Waste") {
                    game.handSize(2) shouldBe initialP2HandSize - 1
                }
            }

            test("shroud still blocks targeted spells even with Words of Waste in play") {
                // Verify that while Words of Waste bypasses shroud (non-targeting),
                // actual targeted spells are still blocked by True Believer's shroud
                val game = scenario()
                    .withPlayers("Attacker", "Shroud Player")
                    .withCardOnBattlefield(1, "Words of Waste")
                    .withCardInHand(1, "Scorching Spear")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "True Believer")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Try to cast Scorching Spear targeting the opponent (who has shroud)
                val castResult = game.castSpellTargetingPlayer(1, "Scorching Spear", 2)

                withClue("Scorching Spear should fail to target player with shroud") {
                    castResult.error shouldBe "Target player has shroud"
                }
            }
        }
    }
}
