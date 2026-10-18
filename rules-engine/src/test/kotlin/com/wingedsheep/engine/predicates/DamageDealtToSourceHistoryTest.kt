package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Per-source damage history read back after the damaged creature has died:
 *  - `StatePredicate.DealtDamageToSourceThisTurn` — "each creature that dealt damage to it this
 *    turn" (Brine Hag), read off each candidate's own record so it answers once the source is gone;
 *  - `DynamicAmount.LastKnownDamageDealtToSource(sourceFilter, excludeSelf)` — the damage dealt to
 *    the source this turn, summed per source from the snapshot captured as it left the battlefield,
 *    optionally narrowed to matching sources ("by other sources named Blazing Effigy").
 */
class DamageDealtToSourceHistoryTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val dealtToSource = GameObjectFilter.Creature.dealtDamageToSourceThisTurn()

    fun GameTestDriver.matches(candidate: EntityId, sourceId: EntityId?) =
        services.predicateEvaluator.matches(
            state, state.projectedState, candidate, dealtToSource,
            PredicateContext.fromEffectContext(EffectContext(sourceId = sourceId, controllerId = player1))
        )

    fun GameTestDriver.run(effect: Effect, controllerId: EntityId, sourceId: EntityId?, target: EntityId): List<GameEvent> {
        val result = services.effectExecutorRegistry.execute(
            state, effect,
            EffectContext(sourceId = sourceId, controllerId = controllerId, targets = listOf(ChosenTarget.Permanent(target)))
        )
        result.error shouldBe null
        replaceState(result.state)
        return result.events
    }

    fun GameTestDriver.ping(by: EntityId, source: EntityId, target: EntityId, amount: Int = 1) =
        run(Effects.DealDamage(amount, EffectTarget.ContextTarget(0)), by, source, target)

    /** Destroy [victim] and evaluate [amount] as its dies trigger would, from the leave event. */
    fun GameTestDriver.evaluateOnDeath(controller: EntityId, victim: EntityId, amount: DynamicAmount): Int {
        val events = run(Effects.Destroy(EffectTarget.ContextTarget(0)), controller, null, victim)
        val leave = events.filterIsInstance<ZoneChangeEvent>().single { it.entityId == victim }
        return services.dynamicAmountEvaluator.evaluate(
            state, amount,
            EffectContext(sourceId = victim, controllerId = controller, triggerContext = TriggerContext.fromEvent(leave))
        )
    }

    context("DealtDamageToSourceThisTurn") {
        test("matches the creature that damaged the source, and only it") {
            val d = driver(); val me = d.player1; val opp = d.getOpponent(me)
            val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")
            val pinger = d.putCreatureOnBattlefield(me, "Grizzly Bears")
            val bystander = d.putCreatureOnBattlefield(me, "Savannah Lions")

            withClue("nothing has dealt damage yet") { d.matches(pinger, victim) shouldBe false }
            d.ping(me, pinger, victim)

            d.matches(pinger, victim) shouldBe true
            d.matches(bystander, victim) shouldBe false
            withClue("source-relative: inert with no source") { d.matches(pinger, null) shouldBe false }
            withClue("the victim isn't a creature that dealt damage to the pinger") {
                d.matches(victim, pinger) shouldBe false
            }
        }

        test("keeps matching after the damaged source has died") {
            val d = driver(); val me = d.player1; val opp = d.getOpponent(me)
            val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")
            val pinger = d.putCreatureOnBattlefield(me, "Grizzly Bears")
            d.ping(me, pinger, victim)
            d.run(Effects.Destroy(EffectTarget.ContextTarget(0)), me, null, victim)

            d.state.getGraveyard(opp).contains(victim) shouldBe true
            d.matches(pinger, victim) shouldBe true
        }

        test("the record ends at cleanup") {
            val d = driver(); val me = d.player1; val opp = d.getOpponent(me)
            val victim = d.putCreatureOnBattlefield(opp, "Centaur Courser")
            val pinger = d.putCreatureOnBattlefield(me, "Grizzly Bears")
            d.ping(me, pinger, victim)

            d.passPriorityUntil(Step.UPKEEP)
            d.matches(pinger, victim) shouldBe false
        }
    }

    context("LastKnownDamageDealtToSource") {
        test("sums every hit, repeated hits from one source included") {
            val d = driver(); val me = d.player1; val opp = d.getOpponent(me)
            val victim = d.putCreatureOnBattlefield(opp, "Force of Nature")
            val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
            val lions = d.putCreatureOnBattlefield(me, "Savannah Lions")
            d.ping(me, bears, victim, 2)
            d.ping(me, bears, victim, 1)
            d.ping(me, lions, victim, 1)

            d.evaluateOnDeath(opp, victim, DynamicAmounts.lastKnownDamageDealtToSource()) shouldBe 4
        }

        test("a name filter counts only matching sources, judged as they were when they dealt damage") {
            val d = driver(); val me = d.player1; val opp = d.getOpponent(me)
            val victim = d.putCreatureOnBattlefield(opp, "Force of Nature")
            val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
            val lions = d.putCreatureOnBattlefield(me, "Savannah Lions")
            d.ping(me, bears, victim, 2)
            d.ping(me, bears, victim, 1)
            d.ping(me, lions, victim, 1)
            withClue("the source having left doesn't matter") {
                d.run(Effects.Destroy(EffectTarget.ContextTarget(0)), me, null, bears)
            }

            d.evaluateOnDeath(opp, victim, DynamicAmounts.lastKnownDamageDealtToSource(
                sourceFilter = GameObjectFilter.Any.named("Grizzly Bears"))) shouldBe 3
            withClue("no matching source counts nothing") {
                val d2 = driver(); val me2 = d2.player1; val opp2 = d2.getOpponent(me2)
                val v2 = d2.putCreatureOnBattlefield(opp2, "Force of Nature")
                d2.ping(me2, d2.putCreatureOnBattlefield(me2, "Savannah Lions"), v2, 2)
                d2.evaluateOnDeath(opp2, v2, DynamicAmounts.lastKnownDamageDealtToSource(
                    sourceFilter = GameObjectFilter.Any.named("Grizzly Bears"))) shouldBe 0
            }
        }

        test("excludeSelf drops the damage the source dealt to itself") {
            val d = driver(); val me = d.player1; val opp = d.getOpponent(me)
            val victim = d.putCreatureOnBattlefield(opp, "Force of Nature")
            val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
            d.ping(opp, victim, victim, 2)
            d.ping(me, bears, victim, 1)

            d.evaluateOnDeath(opp, victim, DynamicAmounts.lastKnownDamageDealtToSource(excludeSelf = true)) shouldBe 1
        }
    }
})
