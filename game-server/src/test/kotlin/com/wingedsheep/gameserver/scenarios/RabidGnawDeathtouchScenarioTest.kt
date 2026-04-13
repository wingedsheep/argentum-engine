package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tests that deathtouch applies when a creature deals damage through Rabid Gnaw.
 *
 * Rabid Gnaw: {1}{R} — Target creature you control gets +1/+0 until end of turn.
 * Then it deals damage equal to its power to target creature you don't control.
 *
 * When the damage source has deathtouch (e.g., Ruthless Ripper), the damage
 * should be lethal regardless of the target's toughness (Rule 704.5h).
 */
class RabidGnawDeathtouchScenarioTest : ScenarioTestBase() {

    init {
        context("Rabid Gnaw with deathtouch creature") {

            test("deathtouch creature dealing damage via Rabid Gnaw destroys large creature") {
                // Ruthless Ripper is 1/1 with deathtouch.
                // Rabid Gnaw gives +1/+0 (making it 2/1), then it deals 2 damage.
                // Even though 2 < 5, deathtouch makes any amount of damage lethal.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ruthless Ripper", summoningSickness = false)
                    .withCardInHand(1, "Rabid Gnaw")
                    .withLandsOnBattlefield(1, "Mountain", 2) // {1}{R}
                    .withCardOnBattlefield(2, "Barkhide Mauler") // 4/4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ripperId = game.findPermanent("Ruthless Ripper")!!
                val maulerId = game.findPermanent("Barkhide Mauler")!!

                // Cast Rabid Gnaw targeting Ruthless Ripper (our creature) and Barkhide Mauler (their creature)
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.state.getHand(game.player1Id).first { entityId ->
                            game.state.getEntity(entityId)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Rabid Gnaw"
                        },
                        targets = listOf(
                            ChosenTarget.Permanent(ripperId),
                            ChosenTarget.Permanent(maulerId)
                        )
                    )
                )
                withClue("Casting Rabid Gnaw should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Barkhide Mauler (4/4) took only 2 damage from Ruthless Ripper,
                // but deathtouch means any damage is lethal
                withClue("Barkhide Mauler should be destroyed by deathtouch damage") {
                    game.isOnBattlefield("Barkhide Mauler") shouldBe false
                }
                withClue("Barkhide Mauler should be in player 2's graveyard") {
                    game.isInGraveyard(2, "Barkhide Mauler") shouldBe true
                }

                // Ruthless Ripper should survive (it wasn't targeted for damage)
                withClue("Ruthless Ripper should still be on the battlefield") {
                    game.isOnBattlefield("Ruthless Ripper") shouldBe true
                }
            }

            test("non-deathtouch creature dealing damage via Rabid Gnaw does not kill large creature") {
                // Raging Goblin is 1/1 without deathtouch.
                // Rabid Gnaw gives +1/+0 (making it 2/1), then it deals 2 damage.
                // 2 damage is not enough to kill a 4/4.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Raging Goblin", summoningSickness = false)
                    .withCardInHand(1, "Rabid Gnaw")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Barkhide Mauler") // 4/4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goblinId = game.findPermanent("Raging Goblin")!!
                val maulerId = game.findPermanent("Barkhide Mauler")!!

                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.state.getHand(game.player1Id).first { entityId ->
                            game.state.getEntity(entityId)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Rabid Gnaw"
                        },
                        targets = listOf(
                            ChosenTarget.Permanent(goblinId),
                            ChosenTarget.Permanent(maulerId)
                        )
                    )
                )
                withClue("Casting Rabid Gnaw should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // 2 damage is not lethal to a 4/4 without deathtouch
                withClue("Barkhide Mauler should survive 2 damage without deathtouch") {
                    game.isOnBattlefield("Barkhide Mauler") shouldBe true
                }
            }
        }
    }
}
