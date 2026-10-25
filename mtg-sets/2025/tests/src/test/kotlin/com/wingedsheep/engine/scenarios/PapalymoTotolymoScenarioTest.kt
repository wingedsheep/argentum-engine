package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fic.cards.PapalymoTotolymo
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Papalymo Totolymo (FIC #90).
 *
 *   Whenever you cast a noncreature spell, Papalymo Totolymo deals 1 damage to each opponent and
 *   you gain 1 life.
 *   {4}, {T}, Sacrifice Papalymo Totolymo: Each opponent who lost life this turn sacrifices a
 *   creature with the greatest power among creatures they control.
 *
 * Pins the activated ability's two gates: only an opponent who lost life this turn sacrifices, and
 * what they sacrifice is restricted to their greatest-power creature.
 */
class PapalymoTotolymoScenarioTest : ScenarioTestBase() {

    private fun papalymoGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Papalymo Totolymo", summoningSickness = false)
        .withLandsOnBattlefield(1, "Plains", 4)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInHand(1, "Lightning Bolt")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activatePapalymo() {
        val papalymo = findPermanent("Papalymo Totolymo")!!
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = papalymo,
                abilityId = PapalymoTotolymo.activatedAbilities[0].id,
            )
        )
        withClue("Activating Papalymo should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    init {
        context("Papalymo Totolymo") {

            test("casting a noncreature spell drains 1, and an opponent who lost life sacrifices their greatest-power creature") {
                val game = papalymoGame()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                withClue("Bolt (3) plus Papalymo's trigger (1); you gained 1") {
                    game.getLifeTotal(2) shouldBe 16
                    game.getLifeTotal(1) shouldBe 21
                }

                game.activatePapalymo()
                withClue("Only Hill Giant has the greatest power, so the opponent has no choice to make") {
                    game.hasPendingDecision() shouldBe false
                }

                withClue("Hill Giant (power 3) was sacrificed; Grizzly Bears stays") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Papalymo was sacrificed as a cost") {
                    game.isOnBattlefield("Papalymo Totolymo") shouldBe false
                }
            }

            test("an opponent who lost no life this turn sacrifices nothing") {
                val game = papalymoGame()

                game.activatePapalymo()

                withClue("No life lost, no sacrifice") {
                    game.hasPendingDecision() shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
