package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.MaximumHandSize
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.player.PlayerMaximumHandSizeReductionComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.ModifyMaximumHandSize
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ModifyMaximumHandSize] — "Each opponent's maximum hand size is reduced by seven" (Jin-Gitaxias,
 * Core Augur) — joins the set / no-maximum effects in CR 613.11 timestamp order: it adjusts the
 * limit standing at its timestamp, a later set overrides it, it can't reduce "no maximum", and it
 * comes from projection, so a granted one counts and a stripped one doesn't.
 */
class ModifyMaximumHandSizeTest : FunSpec({
    val reducer = card("Test Opponents Minus Seven") {
        typeLine = "Enchantment"
        staticAbility { ability = ModifyMaximumHandSize(Player.EachOpponent, DynamicAmount.Fixed(-7)) }
    }
    val plusTwo = card("Test You Plus Two") {
        typeLine = "Enchantment"
        staticAbility { ability = ModifyMaximumHandSize(Player.You, DynamicAmount.Fixed(2)) }
    }
    val twenty = card("Test Everyone Twenty") {
        typeLine = "Enchantment"
        staticAbility { ability = SetMaximumHandSize(Player.Each, DynamicAmount.Fixed(20)) }
    }
    val tower = card("Test Mod No-Max Tower") {
        typeLine = "Artifact"
        staticAbility { ability = NoMaximumHandSize() }
    }
    val lord = card("Test Reducer Lord") {
        typeLine = "Enchantment"
        staticAbility {
            ability = GrantStaticAbility(
                ModifyMaximumHandSize(Player.EachOpponent, DynamicAmount.Fixed(-1)),
                filter = GroupFilter(GameObjectFilter.Creature.youControl())
            )
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(reducer, plusTwo, twenty, tower, lord))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.max(playerId: EntityId): Int? = MaximumHandSize.effective(
        state, playerId, cardRegistry, services.conditionEvaluator, services.conditionEvaluator.amounts
    )

    fun GameTestDriver.put(controller: EntityId, name: String, timestamp: Long): EntityId =
        putPermanentOnBattlefield(controller, name).also {
            addComponent(it, BattlefieldEntryTimestampComponent(timestamp))
        }

    test("reduces each opponent's limit and leaves its controller's alone") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(me, reducer.name, 1)
        d.max(opp) shouldBe 0
        d.max(me) shouldBe 7
    }

    test("an increase raises its controller's limit") {
        val d = driver(); val me = d.activePlayer!!
        d.put(me, plusTwo.name, 1)
        d.max(me) shouldBe 9
    }

    test("applied after an earlier set, it adjusts the set value") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(me, twenty.name, 1)
        d.put(me, reducer.name, 2)
        d.max(opp) shouldBe 13
    }

    test("a later set overrides an earlier reduction") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(me, reducer.name, 1)
        d.put(me, twenty.name, 2)
        d.max(opp) shouldBe 20
    }

    test("no maximum hand size has nothing to reduce, whichever came first") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(opp, tower.name, 1)
        d.put(me, reducer.name, 2)
        d.max(opp) shouldBe null

        val d2 = driver(); val me2 = d2.activePlayer!!; val opp2 = d2.getOpponent(me2)
        d2.put(me2, reducer.name, 1)
        d2.put(opp2, tower.name, 2)
        d2.max(opp2) shouldBe null
    }

    test("stacked reductions and a rest-of-game reduction never go below zero") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(me, reducer.name, 1)
        d.put(me, reducer.name, 2)
        d.addComponent(opp, PlayerMaximumHandSizeReductionComponent(3))
        d.max(opp) shouldBe 0
    }

    test("a granted modification counts per holder, from the holder's controller") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(me, lord.name, 1)
        d.max(opp) shouldBe 7
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.max(opp) shouldBe 5
        d.max(me) shouldBe 7
    }

    test("losing all abilities removes the reduction") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val reducerId = d.put(me, reducer.name, 1)
        d.max(opp) shouldBe 0
        val result = d.services.effectExecutorRegistry.execute(
            d.state,
            Effects.RemoveAllAbilities(EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = reducerId, controllerId = me, targets = listOf(ChosenTarget.Permanent(reducerId)))
        )
        result.error shouldBe null
        d.replaceState(result.state)
        d.max(opp) shouldBe 7
    }
})
