package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ancestral Katana (NEO #1) — "Whenever a Samurai or Warrior you control attacks alone, you may pay
 * {1}. When you do, attach this Equipment to it. Equipped creature gets +2/+1. Equip {3}"
 *
 * Paying {1} sets off a reflexive trigger that attaches the Katana to the lone attacker (Peerless
 * Samurai, 2/3 → 4/4); declining leaves it unattached and the land untapped; attacking with two
 * creatures doesn't trigger it at all.
 */
class AncestralKatanaScenarioTest : ScenarioTestBase() {

    private fun TestGame.untappedPlains(): Int =
        state.getBattlefield(player1Id).count { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == "Plains" &&
                state.getEntity(id)?.get<TappedComponent>() == null
        }

    private fun TestGame.attachedTo(katana: EntityId): EntityId? =
        state.getEntity(katana)?.get<AttachedToComponent>()?.targetId

    /** Answer the attack trigger's "pay {1}?" with [pay] until the stack settles; returns the prompt count. */
    private fun TestGame.runAttackTrigger(pay: Boolean): Int {
        var prompts = 0
        var guard = 0
        while (guard++ < 40) {
            when (val decision = state.pendingDecision) {
                is YesNoDecision -> {
                    prompts++
                    answerYesNo(pay)
                }
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                null -> {
                    if (state.stack.isEmpty()) return prompts
                    resolveStack()
                }
                else -> error("unexpected decision: $decision")
            }
        }
        error("decision loop did not settle")
    }

    init {
        context("Ancestral Katana") {

            test("a Samurai attacking alone: paying {1} attaches the Katana to it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Peerless Samurai")
                    .withCardOnBattlefield(1, "Ancestral Katana")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val samurai = game.findPermanent("Peerless Samurai")!!
                val katana = game.findPermanent("Ancestral Katana")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Peerless Samurai" to 2)).error shouldBe null

                game.runAttackTrigger(pay = true) shouldBe 1

                withClue("the Katana is attached to the lone attacker") {
                    game.attachedTo(katana) shouldBe samurai
                }
                withClue("the {1} was paid") {
                    game.untappedPlains() shouldBe 0
                }
                game.state.projectedState.getPower(samurai) shouldBe 4
                game.state.projectedState.getToughness(samurai) shouldBe 4
            }

            test("declining the {1} leaves the Katana unattached") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Peerless Samurai")
                    .withCardOnBattlefield(1, "Ancestral Katana")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val samurai = game.findPermanent("Peerless Samurai")!!
                val katana = game.findPermanent("Ancestral Katana")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Peerless Samurai" to 2)).error shouldBe null

                game.runAttackTrigger(pay = false) shouldBe 1

                game.attachedTo(katana) shouldBe null
                game.untappedPlains() shouldBe 1
                game.state.projectedState.getPower(samurai) shouldBe 2
            }

            test("a Samurai attacking alongside another creature doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Peerless Samurai")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Ancestral Katana")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val katana = game.findPermanent("Ancestral Katana")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Peerless Samurai" to 2, "Grizzly Bears" to 2)).error shouldBe null

                game.runAttackTrigger(pay = true) shouldBe 0
                game.attachedTo(katana) shouldBe null
            }
        }
    }
}
