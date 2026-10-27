package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Sure-Footed Infiltrator (ZNR #83) — {3}{U} Creature — Merfolk Rogue, 2/3.
 *
 *   Tap another untapped Rogue you control: This creature can't be blocked this turn.
 *   Whenever this creature deals combat damage to a player, draw a card.
 *
 * Pins the tap cost's filter (another Rogue — not a non-Rogue, not the Infiltrator itself), that the
 * evasion holds through blocks, and the combat-damage draw.
 */
class SureFootedInfiltratorScenarioTest : ScenarioTestBase() {

    init {
        context("Sure-Footed Infiltrator") {

            test("tapping another Rogue makes it unblockable, and connecting draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sure-Footed Infiltrator")
                    .withCardOnBattlefield(1, "Jaspera Sentinel") // Elf Rogue
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val infiltrator = game.findPermanent("Sure-Footed Infiltrator")!!
                val sentinel = game.findPermanent("Jaspera Sentinel")!!
                val abilityId = cardRegistry.getCard("Sure-Footed Infiltrator")!!.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = infiltrator,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(sentinel))
                    )
                )
                withClue("Tapping the Rogue pays the cost: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("the Rogue was tapped, the Infiltrator was not") {
                    game.state.getEntity(sentinel)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(infiltrator)?.has<TappedComponent>() shouldBe false
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Sure-Footed Infiltrator" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("the Infiltrator can't be blocked this turn") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Sure-Footed Infiltrator")))
                        .error shouldNotBe null
                }
                game.declareNoBlockers().error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                }
                game.resolveStack()

                withClue("2 combat damage to the opponent, and the Infiltrator's controller draws") {
                    game.getLifeTotal(2) shouldBe 18
                    game.isInHand(1, "Island") shouldBe true
                }
            }

            test("a non-Rogue, or the Infiltrator itself, cannot pay the tap cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sure-Footed Infiltrator")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val infiltrator = game.findPermanent("Sure-Footed Infiltrator")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Sure-Footed Infiltrator")!!.activatedAbilities.first().id

                withClue("Grizzly Bears is not a Rogue") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = infiltrator,
                            abilityId = abilityId,
                            costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears))
                        )
                    ).error shouldNotBe null
                }
                withClue("\"another\" Rogue excludes the Infiltrator itself") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = infiltrator,
                            abilityId = abilityId,
                            costPayment = AdditionalCostPayment(tappedPermanents = listOf(infiltrator))
                        )
                    ).error shouldNotBe null
                }
            }
        }
    }
}
