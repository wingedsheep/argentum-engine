package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rakalite (ATQ #62).
 *
 * {6} Artifact
 * "{2}: Prevent the next 1 damage that would be dealt to any target this turn. Return this
 *  artifact to its owner's hand at the beginning of the next end step."
 */
class RakaliteScenarioTest : ScenarioTestBase() {

    // A {0} sorcery that deals 2 damage to a target creature — the source whose damage the
    // Rakalite shield will partially prevent.
    private val twoBolt = card("Two Bolt") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Deal 2 damage to target creature."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.DealDamage(2, t)
        }
    }

    init {
        cardRegistry.register(twoBolt)

        context("Rakalite") {

            test("prevents exactly 1 damage to a creature, then returns to hand at the next end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rakalite")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardInHand(1, "Two Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2) // pays {2}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rakaliteId = game.findPermanent("Rakalite")!!
                val giantId = game.findPermanent("Hill Giant")!!
                val abilityId = cardRegistry.getCard("Rakalite")!!.script.activatedAbilities[0].id

                // Activate Rakalite targeting Hill Giant; auto-pay the {2}.
                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rakaliteId,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, giantId))
                    )
                )
                withClue("Activating Rakalite should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                // Pay {2} if a mana-source decision is presented; then resolve.
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                // Now deal 2 damage to Hill Giant — the shield prevents 1, so only 1 lands.
                game.castSpell(1, "Two Bolt", giantId).error shouldBe null
                game.resolveStack()

                val damage = game.state.getEntity(giantId)?.get<DamageComponent>()?.amount ?: 0
                withClue("Rakalite prevents 1 of the 2 damage, so Hill Giant has marked 1 damage") {
                    damage shouldBe 1
                }
                withClue("Hill Giant (3/3) survives 1 damage") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }

                // Advance to the end step: the delayed trigger returns Rakalite to its owner's hand.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Rakalite returns to hand at the beginning of the end step") {
                    game.isOnBattlefield("Rakalite") shouldBe false
                    game.isInHand(1, "Rakalite") shouldBe true
                }
            }
        }
    }
}
