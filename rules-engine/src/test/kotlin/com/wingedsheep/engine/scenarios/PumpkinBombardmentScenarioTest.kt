package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Pumpkin Bombardment (SPM).
 *
 * {B/R} Sorcery
 * As an additional cost to cast this spell, discard a card or pay {2}.
 * Pumpkin Bombardment deals 3 damage to target creature.
 *
 * Exercises the discard leg of [com.wingedsheep.sdk.scripting.AdditionalCost.OrPay] (discard path
 * vs pay path), including the case where an empty hand leaves only the pay path.
 */
class PumpkinBombardmentScenarioTest : ScenarioTestBase() {

    // A 3/3 target that dies to Pumpkin Bombardment's 3 damage, and a spare card to discard.
    private val testBear = card("Pumpkin Test Bear") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
    }
    private val spareCard = card("Pumpkin Test Spare") {
        manaCost = "{1}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
    }

    init {
        cardRegistry.register(listOf(testBear, spareCard))

        context("discard-or-pay additional cost") {
            test("discard path: discards a card and deals 3 damage to the target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pumpkin Test Bear")
                    .withCardInHand(2, "Pumpkin Bombardment")
                    .withCardInHand(2, "Pumpkin Test Spare")
                    .withLandsOnBattlefield(2, "Mountain", 1) // {R} pays {B/R}
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearId = game.state.getBattlefield().first {
                    val c = game.state.getEntity(it)
                    c?.get<CardComponent>()?.name == "Pumpkin Test Bear" &&
                        c.get<ControllerComponent>()?.playerId == game.player1Id
                }
                val pumpkinId = game.state.getHand(game.player2Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Pumpkin Bombardment"
                }
                val spareId = game.state.getHand(game.player2Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Pumpkin Test Spare"
                }

                val result = game.execute(
                    CastSpell(
                        game.player2Id,
                        pumpkinId,
                        listOf(ChosenTarget.Permanent(bearId)),
                        additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(spareId)),
                    )
                )
                withClue("Pumpkin Bombardment should cast via the discard path: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("The spare card should be discarded") {
                    game.isInGraveyard(2, "Pumpkin Test Spare") shouldBe true
                }

                game.resolveStack()

                withClue("The target creature should be destroyed by 3 damage") {
                    game.isInGraveyard(1, "Pumpkin Test Bear") shouldBe true
                }
            }

            test("pay path: no discard, engine adds {2}, and deals 3 damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pumpkin Test Bear")
                    .withCardInHand(2, "Pumpkin Bombardment")
                    .withCardInHand(2, "Pumpkin Test Spare")
                    .withLandsOnBattlefield(2, "Mountain", 3) // enough for {B/R} + {2}
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearId = game.state.getBattlefield().first {
                    val c = game.state.getEntity(it)
                    c?.get<CardComponent>()?.name == "Pumpkin Test Bear" &&
                        c.get<ControllerComponent>()?.playerId == game.player1Id
                }
                val pumpkinId = game.state.getHand(game.player2Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Pumpkin Bombardment"
                }

                // Pay path: no discard payment — the engine adds {2} to the cost.
                val result = game.execute(
                    CastSpell(game.player2Id, pumpkinId, listOf(ChosenTarget.Permanent(bearId)))
                )
                withClue("Pumpkin Bombardment should cast via the pay path: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("The spare card should still be in hand (no discard on the pay path)") {
                    game.isInHand(2, "Pumpkin Test Spare") shouldBe true
                }

                game.resolveStack()

                withClue("The target creature should be destroyed by 3 damage") {
                    game.isInGraveyard(1, "Pumpkin Test Bear") shouldBe true
                }
            }

            test("with an empty hand, only the pay path is castable") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pumpkin Test Bear")
                    .withCardInHand(2, "Pumpkin Bombardment")
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pumpkinActions = game.getLegalActions(2)
                    .filter { it.description.startsWith("Cast Pumpkin Bombardment") }
                withClue("Pumpkin Bombardment should be castable (pay path)") {
                    pumpkinActions.isNotEmpty() shouldBe true
                }
                withClue("No discard path should be offered with no other card in hand") {
                    pumpkinActions.none { it.description.endsWith("(Discard)") } shouldBe true
                }
            }
        }
    }
}
