package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Cruel Revival.
 *
 * Card reference:
 * - Cruel Revival ({4}{B}): Instant
 *   "Destroy target non-Zombie creature. It can't be regenerated.
 *    Return up to one target Zombie card from your graveyard to your hand."
 */
class CruelRevivalScenarioTest : ScenarioTestBase() {

    init {
        context("Cruel Revival destroys creature") {
            test("destroys target creature when no zombie target is available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Cruel Revival")
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3 non-Zombie
                    .withLandsOnBattlefield(2, "Swamp", 5) // {4}{B}
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giantId = game.findPermanent("Hill Giant")!!

                // Cast Cruel Revival targeting only the creature (no zombie target)
                val castResult = game.castSpell(2, "Cruel Revival", giantId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Hill Giant should be destroyed") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Hill Giant should be in player 1's graveyard") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
            }

            test("destroys Symbiotic Elf and creates exactly 2 insect tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Cruel Revival")
                    .withCardOnBattlefield(1, "Symbiotic Elf") // 2/2, creates 2 tokens on death
                    .withLandsOnBattlefield(2, "Swamp", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elfId = game.findPermanent("Symbiotic Elf")!!

                // Cast Cruel Revival targeting Symbiotic Elf (no zombie target)
                val castResult = game.castSpell(2, "Cruel Revival", elfId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Symbiotic Elf should be destroyed") {
                    game.isOnBattlefield("Symbiotic Elf") shouldBe false
                }
                withClue("Symbiotic Elf should be in player 1's graveyard") {
                    game.isInGraveyard(1, "Symbiotic Elf") shouldBe true
                }

                // The die trigger should create exactly 2 insect tokens, not 4
                val insectTokens = game.findAllPermanents("Insect Token")
                withClue("Should create exactly 2 insect tokens from Symbiotic Elf dying") {
                    insectTokens.size shouldBe 2
                }
            }

            test("destroys creature and returns zombie from graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Cruel Revival")
                    .withCardOnBattlefield(1, "Hill Giant") // Non-Zombie target
                    .withCardInGraveyard(2, "Gluttonous Zombie") // Zombie in caster's graveyard
                    .withLandsOnBattlefield(2, "Swamp", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giantId = game.findPermanent("Hill Giant")!!
                val zombieId = game.findCardsInGraveyard(2, "Gluttonous Zombie").first()

                // Cast Cruel Revival with both targets: creature + zombie card
                val playerId = game.player2Id
                val hand = game.state.getHand(playerId)
                val cardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Cruel Revival"
                }!!

                val targets = listOf(
                    ChosenTarget.Permanent(giantId),
                    ChosenTarget.Card(zombieId, playerId, Zone.GRAVEYARD)
                )
                val castResult = game.execute(CastSpell(playerId, cardId, targets))
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Hill Giant should be destroyed") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Hill Giant should be in player 1's graveyard") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                withClue("Gluttonous Zombie should be returned to player 2's hand") {
                    game.isInHand(2, "Gluttonous Zombie") shouldBe true
                }
                withClue("Gluttonous Zombie should no longer be in graveyard") {
                    game.isInGraveyard(2, "Gluttonous Zombie") shouldBe false
                }
            }
        }
    }
}
