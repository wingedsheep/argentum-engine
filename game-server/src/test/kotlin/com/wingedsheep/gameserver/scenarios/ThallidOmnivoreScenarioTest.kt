package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Thallid Omnivore.
 *
 * Card reference:
 * - Thallid Omnivore ({3}{B}): Creature — Fungus, 3/3
 *   "{1}, Sacrifice another creature: Thallid Omnivore gets +2/+2 until end of turn.
 *    If a Saproling was sacrificed this way, you gain 2 life."
 */
class ThallidOmnivoreScenarioTest : ScenarioTestBase() {

    init {
        context("Thallid Omnivore - sacrifice for +2/+2") {

            test("sacrificing a non-Saproling creature gives +2/+2 but no life gain") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thallid Omnivore")
                    .withCardOnBattlefield(1, "Cabal Evangel") // 2/2 Human
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(1)

                val omnivoreId = game.findPermanent("Thallid Omnivore")!!
                val evangelId = game.findPermanent("Cabal Evangel")!!
                val cardDef = cardRegistry.getCard("Thallid Omnivore")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = omnivoreId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(evangelId)
                        )
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Cabal Evangel should be sacrificed
                game.isInGraveyard(1, "Cabal Evangel") shouldBe true

                // Omnivore should be 5/5
                val clientState = game.getClientState(1)
                val omnivoreInfo = clientState.cards[omnivoreId]
                withClue("Thallid Omnivore should be 5/5") {
                    omnivoreInfo shouldNotBe null
                    omnivoreInfo!!.power shouldBe 5
                    omnivoreInfo.toughness shouldBe 5
                }

                // No life gain for non-Saproling sacrifice
                withClue("Life should not change when sacrificing a non-Saproling") {
                    game.getLifeTotal(1) shouldBe initialLife
                }
            }

            test("sacrificing a Saproling token gives +2/+2 and 2 life") {
                // Use Saproling Migration to create Saproling tokens
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thallid Omnivore")
                    .withCardInHand(1, "Saproling Migration")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Saproling Migration to create 2 Saproling tokens
                val castResult = game.castSpell(1, "Saproling Migration")
                withClue("Saproling Migration should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find a Saproling token
                val saprolingId = game.findPermanent("Saproling Token")!!
                val omnivoreId = game.findPermanent("Thallid Omnivore")!!

                val initialLife = game.getLifeTotal(1)

                val cardDef = cardRegistry.getCard("Thallid Omnivore")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = omnivoreId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(saprolingId)
                        )
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Omnivore should be 5/5
                val clientState = game.getClientState(1)
                val omnivoreInfo = clientState.cards[omnivoreId]
                withClue("Thallid Omnivore should be 5/5") {
                    omnivoreInfo shouldNotBe null
                    omnivoreInfo!!.power shouldBe 5
                    omnivoreInfo.toughness shouldBe 5
                }

                // Life should increase by 2 for Saproling sacrifice
                withClue("Life should increase by 2 when sacrificing a Saproling") {
                    game.getLifeTotal(1) shouldBe initialLife + 2
                }
            }
        }
    }
}
