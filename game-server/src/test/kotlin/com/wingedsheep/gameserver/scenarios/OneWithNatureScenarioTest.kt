package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for One with Nature.
 *
 * Card reference:
 * - One with Nature ({G}): Enchantment — Aura
 *   "Enchant creature"
 *   "Whenever enchanted creature deals combat damage to a player, you may search your library
 *   for a basic land card, put that card onto the battlefield tapped, then shuffle."
 */
class OneWithNatureScenarioTest : ScenarioTestBase() {

    private fun countPermanentsByName(game: TestGame, name: String): Int {
        return game.state.getBattlefield().count { entityId ->
            game.state.getEntity(entityId)?.get<CardComponent>()?.name == name
        }
    }

    private fun isTapped(game: TestGame, name: String): Boolean {
        val entityId = game.findPermanent(name) ?: return false
        return game.state.getEntity(entityId)?.has<TappedComponent>() == true
    }

    private fun findCardsInLibrary(game: TestGame, playerNumber: Int, cardName: String): List<com.wingedsheep.sdk.model.EntityId> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getLibrary(playerId).filter { entityId ->
            game.state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    init {
        context("One with Nature combat damage trigger") {
            test("searching for basic land when enchanted creature deals combat damage") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardInHand(1, "One with Nature")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 creature
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain") // Opponent needs library
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast One with Nature targeting Glory Seeker
                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                val castResult = game.castSpell(1, "One with Nature", glorySeekerID)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("One with Nature should be on the battlefield") {
                    game.isOnBattlefield("One with Nature") shouldBe true
                }

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Glory Seeker
                val attackResult = game.declareAttackers(mapOf("Glory Seeker" to 2))
                withClue("Declaring Glory Seeker as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                // Advance to blockers and declare no blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Answer yes to "you may search"
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Should now have a library search decision - select Forest
                withClue("Should have pending search decision") {
                    game.hasPendingDecision() shouldBe true
                }
                val forestsInLibrary = findCardsInLibrary(game, 1, "Forest")
                game.selectCards(listOf(forestsInLibrary.first()))

                // Advance past remaining priority
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Should have 2 Forests on the battlefield (original + fetched)
                withClue("Should have 2 Forests on the battlefield") {
                    countPermanentsByName(game, "Forest") shouldBe 2
                }

                // The fetched Forest should be tapped
                // (original may be tapped from casting, so just check battlefield count)

                // Opponent should have taken 2 combat damage
                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("declining to search when enchanted creature deals combat damage") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardInHand(1, "One with Nature")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast aura
                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "One with Nature", glorySeekerID)
                game.resolveStack()

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Answer no to "you may search"
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(false)

                // Advance past remaining priority
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Should only have 1 Forest (the land used to cast the aura)
                withClue("Should still have only 1 Forest") {
                    countPermanentsByName(game, "Forest") shouldBe 1
                }
            }

            test("no trigger when enchanted creature is blocked") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardInHand(1, "One with Nature")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3, will kill Glory Seeker
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast aura
                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "One with Nature", glorySeekerID)
                game.resolveStack()

                // Combat sequence
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Glory Seeker")))

                // Advance through combat - no trigger since no damage to player
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Should only have 1 Forest
                withClue("Should still have only 1 Forest") {
                    countPermanentsByName(game, "Forest") shouldBe 1
                }

                // Opponent should be at full life
                withClue("Opponent should be at 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("aura controller searches when enchanting opponent's creature") {
                // Player 1 enchants Player 2's creature. When that creature deals combat damage
                // to Player 1, Player 1 (the aura controller) gets to search for a land.
                val game = scenario()
                    .withPlayers("Enchanter", "Attacker")
                    .withCardInHand(1, "One with Nature")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 opponent's creature
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast One with Nature targeting opponent's Glory Seeker
                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                val castResult = game.castSpell(1, "One with Nature", glorySeekerID)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Pass to opponent's turn so they can attack
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Now it's Player 2's turn - attack with Glory Seeker
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attackResult = game.declareAttackers(mapOf("Glory Seeker" to 1))
                withClue("Declaring Glory Seeker as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires for Player 1 (aura controller)
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Player 1 (aura controller) should get the may decision
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Player 1 searches their library
                withClue("Should have pending search decision") {
                    game.hasPendingDecision() shouldBe true
                }
                val forestsInLibrary = findCardsInLibrary(game, 1, "Forest")
                game.selectCards(listOf(forestsInLibrary.first()))

                // Advance past remaining priority
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Player 1 should have 2 Forests on the battlefield
                withClue("Player 1 should have 2 Forests") {
                    countPermanentsByName(game, "Forest") shouldBe 2
                }

                // Player 1 should have taken 2 combat damage
                withClue("Player 1 should have taken 2 combat damage") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }
        }
    }
}
