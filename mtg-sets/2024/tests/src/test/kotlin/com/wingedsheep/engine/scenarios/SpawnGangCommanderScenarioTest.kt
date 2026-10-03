package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spawn-Gang Commander (MH3 #140) — {3}{R}{R} Creature — Eldrazi Goblin 2/2
 *
 *   Devoid
 *   When you cast this spell, create three 0/1 colorless Eldrazi Spawn creature tokens.
 *   {1}{C}, Sacrifice an Eldrazi: This creature deals 2 damage to any target.
 */
class SpawnGangCommanderScenarioTest : ScenarioTestBase() {

    private fun activationBoard(extra: String) = scenario()
        .withPlayers("Player1", "Opponent")
        .withCardOnBattlefield(1, "Spawn-Gang Commander", summoningSickness = false)
        .withCardOnBattlefield(1, extra, summoningSickness = false)
        .withLandsOnBattlefield(1, "Wastes", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Spawn-Gang Commander") {
            test("casting it creates three Eldrazi Spawn") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Spawn-Gang Commander")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Spawn-Gang Commander").error shouldBe null
                game.state.stack shouldHaveSize 2 // the cast trigger sits above the creature spell

                // Resolve only the cast trigger: the Spawn arrive while the Commander is still a spell.
                game.passPriority()
                game.passPriority()
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 3
                game.isOnBattlefield("Spawn-Gang Commander") shouldBe false

                game.resolveStack()
                game.isOnBattlefield("Spawn-Gang Commander") shouldBe true
            }

            test("{1}{C}, sacrifice an Eldrazi deals 2 damage to any target") {
                val game = activationBoard("Nulldrifter")
                val commander = game.findPermanent("Spawn-Gang Commander")!!
                val eldrazi = game.findPermanent("Nulldrifter")!!
                val abilityId = cardRegistry.getCard("Spawn-Gang Commander")!!.activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = commander,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(eldrazi)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.isInGraveyard(1, "Nulldrifter") shouldBe true
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
            }

            test("it can sacrifice itself and still deal the damage") {
                val game = activationBoard("Grizzly Bears")
                val commander = game.findPermanent("Spawn-Gang Commander")!!
                val abilityId = cardRegistry.getCard("Spawn-Gang Commander")!!.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = commander,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(commander)),
                    )
                ).error shouldBe null
                game.isInGraveyard(1, "Spawn-Gang Commander") shouldBe true
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("a non-Eldrazi can't pay the sacrifice cost") {
                val game = activationBoard("Grizzly Bears")
                val commander = game.findPermanent("Spawn-Gang Commander")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Spawn-Gang Commander")!!.activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = commander,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                    )
                )
                result.error shouldNotBe null
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            test("{C} can't be paid with red mana") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Spawn-Gang Commander", summoningSickness = false)
                    .withCardOnBattlefield(1, "Nulldrifter", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val abilityId = cardRegistry.getCard("Spawn-Gang Commander")!!.activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Spawn-Gang Commander")!!,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(game.findPermanent("Nulldrifter")!!)
                        ),
                    )
                )
                result.error shouldNotBe null
                game.isOnBattlefield("Nulldrifter") shouldBe true
            }
        }
    }
}
