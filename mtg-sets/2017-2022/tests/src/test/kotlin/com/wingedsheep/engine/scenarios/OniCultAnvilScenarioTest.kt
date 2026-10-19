package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Oni-Cult Anvil (NEO #230) — {B}{R} Artifact.
 *
 *   Whenever one or more artifacts you control leave the battlefield during your turn, create a
 *   1/1 colorless Construct artifact creature token. This ability triggers only once each turn.
 *   {T}, Sacrifice an artifact: This artifact deals 1 damage to each opponent. You gain 1 life.
 *
 * The "one or more … only once each turn" reading rides on the per-turn trigger cap and the
 * "during your turn" trigger restriction, and the Anvil sacrificing itself relies on the
 * leaves-the-battlefield look-back — all execution-dependent, so each gets a test.
 */
class OniCultAnvilScenarioTest : ScenarioTestBase() {

    private fun activateAnvil(game: TestGame, playerNumber: Int, sacrificed: String) =
        game.execute(
            ActivateAbility(
                playerId = if (playerNumber == 1) game.player1Id else game.player2Id,
                sourceId = game.findPermanent("Oni-Cult Anvil")!!,
                abilityId = cardRegistry.getCard("Oni-Cult Anvil")!!.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(game.findPermanent(sacrificed)!!))
            )
        )

    init {
        context("Oni-Cult Anvil") {

            test("sacrificing an artifact on your turn drains 1 and makes one Construct; a second departure that turn makes none") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Oni-Cult Anvil")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Bottle Gnomes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                activateAnvil(game, 1, "Ornithopter").error shouldBe null
                game.resolveStack()

                withClue("the ability drained the opponent for 1") {
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 21
                }
                withClue("Ornithopter leaving during your turn made one Construct") {
                    game.findPermanents("Construct Token").size shouldBe 1
                }

                val gnomes = game.findPermanent("Bottle Gnomes")!!
                val gnomesAbility = cardRegistry.getCard("Bottle Gnomes")!!.activatedAbilities.single().id
                game.execute(ActivateAbility(game.player1Id, gnomes, gnomesAbility)).error shouldBe null
                game.resolveStack()

                withClue("the trigger already fired this turn, so Bottle Gnomes leaving makes no second Construct") {
                    game.isOnBattlefield("Bottle Gnomes") shouldBe false
                    game.findPermanents("Construct Token").size shouldBe 1
                }
            }

            test("an artifact leaving during an opponent's turn makes no Construct") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Oni-Cult Anvil")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                activateAnvil(game, 1, "Ornithopter").error shouldBe null
                game.resolveStack()

                withClue("the drain still happens at instant speed") {
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 21
                }
                withClue("not your turn — no Construct") {
                    game.findPermanents("Construct Token").size shouldBe 0
                }
            }

            test("sacrificing the Anvil to its own ability still makes a Construct") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Oni-Cult Anvil")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                activateAnvil(game, 1, "Oni-Cult Anvil").error shouldBe null
                game.resolveStack()

                withClue("the Anvil is gone, the drain resolved, and its own departure made a Construct") {
                    game.isOnBattlefield("Oni-Cult Anvil") shouldBe false
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 21
                    game.findPermanents("Construct Token").size shouldBe 1
                }
            }
        }
    }
}
