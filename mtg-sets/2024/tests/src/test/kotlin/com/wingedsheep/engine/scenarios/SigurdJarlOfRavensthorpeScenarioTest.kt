package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderTriggersDecision
import com.wingedsheep.engine.core.TriggerOrderResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sigurd, Jarl of Ravensthorpe (ACR #66) — {R}{G}{W} Legendary Creature — Human Warrior, 3/3.
 *
 *   Vigilance, trample, lifelink
 *   Boast — {1}: Put a lore counter on target Saga you control or remove one from it.
 *   Whenever you put a lore counter on a Saga you control, put a +1/+1 counter on up to one
 *   other target creature.
 *
 * Pins that the lore counters you put fire the trigger — a Saga's entry counter and the boast —
 * and that removing one doesn't. The precombat-main turn-based counter is pinned engine-side by
 * SagaLoreCounterPlacerTest.
 */
class SigurdJarlOfRavensthorpeScenarioTest : ScenarioTestBase() {

    init {
        context("Sigurd, Jarl of Ravensthorpe") {

            fun TestGame.plusOnes(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            fun TestGame.lore(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0

            /** Resolve the stack, aiming Sigurd's trigger at [target] and ordering triggers as offered. */
            fun TestGame.resolveAimingAt(target: EntityId) {
                var guard = 0
                while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 20) {
                    when (val decision = state.pendingDecision) {
                        is ChooseTargetsDecision -> selectTargets(listOf(target))
                        is OrderTriggersDecision ->
                            submitDecision(TriggerOrderResponse(decision.id, decision.triggers.indices.toList()))
                        null -> resolveStack()
                        else -> error("unexpected decision $decision")
                    }
                }
            }

            val boastId by lazy {
                cardRegistry.getCard("Sigurd, Jarl of Ravensthorpe")!!.activatedAbilities.single { it.isBoast }.id
            }

            test("a Saga entering with its lore counter triggers Sigurd") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sigurd, Jarl of Ravensthorpe")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "The Flame of Keld")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "The Flame of Keld").error shouldBe null
                game.resolveAimingAt(bears)

                withClue("Grizzly Bears got a +1/+1 counter from the entry lore counter") {
                    game.plusOnes(bears) shouldBe 1
                }
            }

            test("boast puts a lore counter (trigger) or removes one (no trigger)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sigurd, Jarl of Ravensthorpe")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withCardOnBattlefield(1, "The Flame of Keld")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val sigurd = game.findPermanent("Sigurd, Jarl of Ravensthorpe")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val saga = game.findPermanent("The Flame of Keld")!!

                game.declareAttackers(mapOf("Sigurd, Jarl of Ravensthorpe" to 2)).error shouldBe null
                game.execute(
                    ActivateAbility(game.player1Id, sigurd, boastId, listOf(ChosenTarget.Permanent(saga)))
                ).error shouldBe null
                game.resolveAimingAt(bears)

                withClue("no lore counter yet, so the boast just put one") { game.lore(saga) shouldBe 1 }
                withClue("putting it triggered Sigurd") { game.plusOnes(bears) shouldBe 1 }
            }

            test("boast removing a lore counter doesn't trigger Sigurd") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sigurd, Jarl of Ravensthorpe")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "The Flame of Keld")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val sigurd = game.findPermanent("Sigurd, Jarl of Ravensthorpe")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "The Flame of Keld").error shouldBe null
                game.resolveAimingAt(bears)
                val saga = game.findPermanent("The Flame of Keld")!!
                game.plusOnes(bears) shouldBe 1

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Sigurd, Jarl of Ravensthorpe" to 2)).error shouldBe null
                game.execute(
                    ActivateAbility(game.player1Id, sigurd, boastId, listOf(ChosenTarget.Permanent(saga)))
                ).error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected the put-or-remove choice; got ${game.getPendingDecision()}")
                val remove = choice.options.indexOfFirst { it.startsWith("Remove") }
                game.submitDecision(OptionChosenResponse(choice.id, optionIndex = remove))
                game.resolveStack()

                withClue("the lore counter was removed") { game.lore(saga) shouldBe 0 }
                withClue("removing doesn't trigger Sigurd") { game.plusOnes(bears) shouldBe 1 }
            }
        }
    }
}
