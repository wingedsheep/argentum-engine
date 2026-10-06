package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Whenever a creature dealt damage by this creature this turn dies" (`Triggers.self.damagedCreatureDies`)
 * when the damaging creature leaves the battlefield in the same event as its victim.
 *
 * CR 603.10a: leaves-the-battlefield abilities "look back in time", so the trigger still fires
 * for every creature it damaged that died alongside it — the Dread Slaver ruling (2012-05-01) for
 * a combat trade, and its "dealt damage to itself and then dies" ruling for the source itself.
 * A creature that died in the same event but was never damaged by the source must not fire it,
 * and a source that survives must fire through the live path exactly once per victim.
 */
class DamagedCreatureDiesLookBackTest : FunSpec({

    // {0}: deals 5 damage to each creature. Gains 1 life per creature it damaged that dies.
    val fragileSweeper = card("Fragile Sweeper") {
        manaCost = "{0}"
        typeLine = "Creature — Elemental"
        power = 1
        toughness = 3
        activatedAbility {
            cost = Costs.Free
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.DealDamage(5, EffectTarget.IterationEntity)
            )
        }
        triggeredAbility {
            trigger = Triggers.self.damagedCreatureDies()
            effect = Effects.GainLife(1)
        }
    }

    // The same, but tough enough to survive its own sweep.
    val sturdySweeper = card("Sturdy Sweeper") {
        manaCost = "{0}"
        typeLine = "Creature — Elemental"
        power = 1
        toughness = 10
        activatedAbility {
            cost = Costs.Free
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.DealDamage(5, EffectTarget.IterationEntity)
            )
        }
        triggeredAbility {
            trigger = Triggers.self.damagedCreatureDies()
            effect = Effects.GainLife(1)
        }
    }

    // {0}: deals 1 damage to target creature. Gains 1 life per creature it damaged that dies.
    val pinger = card("Look-back Pinger") {
        manaCost = "{0}"
        typeLine = "Creature — Elemental"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Free
            val creature = target(TargetFilter.Creature)
            effect = Effects.DealDamage(1, creature)
        }
        triggeredAbility {
            trigger = Triggers.self.damagedCreatureDies()
            effect = Effects.GainLife(1)
        }
    }

    // A spell, so the creatures it kills were damaged by no creature.
    val sweep = card("Two-Point Sweep") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.DealDamage(2, EffectTarget.IterationEntity)
            )
        }
    }

    val bear = card("Look-back Bear") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(fragileSweeper, sturdySweeper, pinger, sweep, bear))
        it.initMirrorMatch(deck = Deck.of("Plains" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveAll() {
        repeat(30) {
            if (state.pendingDecision != null) autoResolveDecision()
            else if (stackSize > 0) bothPass()
            else return
        }
        error("stack did not finish resolving")
    }

    test("a source that dies with its victims fires for each of them, itself included") {
        val d = driver()
        val me = d.player1
        val sweeper = d.putCreatureOnBattlefield(me, "Fragile Sweeper")
        d.putCreatureOnBattlefield(me, "Look-back Bear")
        d.putCreatureOnBattlefield(d.player2, "Look-back Bear")

        d.submitSuccess(ActivateAbility(me, sweeper, fragileSweeper.activatedAbilities.first().id))
        d.resolveAll()

        withClue("everything died in one event") {
            d.getCreatures(me).size shouldBe 0
            d.getCreatures(d.player2).size shouldBe 0
        }
        withClue("two Bears plus the Sweeper that damaged itself: three triggers") {
            d.getLifeTotal(me) shouldBe 23
        }
    }

    test("a source that survives fires once per victim through the live path, never twice") {
        val d = driver()
        val me = d.player1
        val sweeper = d.putCreatureOnBattlefield(me, "Sturdy Sweeper")
        d.putCreatureOnBattlefield(me, "Look-back Bear")
        d.putCreatureOnBattlefield(d.player2, "Look-back Bear")

        d.submitSuccess(ActivateAbility(me, sweeper, sturdySweeper.activatedAbilities.first().id))
        d.resolveAll()

        d.findPermanent(me, "Sturdy Sweeper") shouldBe sweeper
        d.getLifeTotal(me) shouldBe 22
    }

    test("a creature dying alongside the source fires it only if the source had damaged it") {
        val d = driver()
        val me = d.player1
        val source = d.putCreatureOnBattlefield(me, "Look-back Pinger")
        val damaged = d.putCreatureOnBattlefield(d.player2, "Look-back Bear")
        d.putCreatureOnBattlefield(d.player2, "Look-back Bear")

        d.submitSuccess(
            ActivateAbility(me, source, pinger.activatedAbilities.first().id, targets = listOf(ChosenTarget.Permanent(damaged)))
        )
        d.resolveAll()
        d.getLifeTotal(me) shouldBe 20

        // The spell kills the Pinger, the Bear it pinged, and an undamaged Bear in one event.
        d.castSpell(me, d.putCardInHand(me, "Two-Point Sweep")).error shouldBe null
        d.resolveAll()

        withClue("all three died together") {
            d.getCreatures(me).size shouldBe 0
            d.getCreatures(d.player2).size shouldBe 0
        }
        withClue("only the pinged Bear fires it — not the undamaged Bear, not the Pinger itself") {
            d.getLifeTotal(me) shouldBe 21
        }
    }
})
