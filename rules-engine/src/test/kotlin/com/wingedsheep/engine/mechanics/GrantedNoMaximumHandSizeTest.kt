package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.MaximumHandSize
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "You have no maximum hand size" reaches a player through a permanent that *has* the ability,
 * however it got it: printed, gained from a resolved effect, or granted by a
 * [GrantStaticAbility] static ("Enchanted creature has 'You have no maximum hand size'" — Nerd
 * Rage). The "you" is the holder's controller, the ability goes away with the holder's abilities,
 * and against a set-maximum effect it is dated by the later of holder and granter (CR 613.7a).
 */
class GrantedNoMaximumHandSizeTest : FunSpec({
    val aura = card("Test No-Max Aura") {
        typeLine = "Enchantment — Aura"
        staticAbility { ability = GrantStaticAbility(NoMaximumHandSize, filter = Filters.EnchantedCreature) }
    }
    val lord = card("Test No-Max Lord") {
        typeLine = "Enchantment"
        staticAbility {
            ability = GrantStaticAbility(NoMaximumHandSize, filter = GroupFilter(GameObjectFilter.Creature.youControl()))
        }
    }
    val tower = card("Test No-Max Tower") {
        typeLine = "Artifact"
        staticAbility { ability = NoMaximumHandSize }
    }
    val twenty = card("Test Twenty Max") {
        typeLine = "Enchantment"
        staticAbility { ability = SetMaximumHandSize(amount = DynamicAmount.Fixed(20)) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(aura, lord, tower, twenty))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.max(playerId: EntityId): Int? = MaximumHandSize.effective(
        state, playerId, cardRegistry, services.conditionEvaluator, services.conditionEvaluator.amounts
    )

    fun GameTestDriver.stamp(entityId: EntityId, timestamp: Long) =
        addComponent(entityId, BattlefieldEntryTimestampComponent(timestamp))

    fun GameTestDriver.enchant(controller: EntityId, creature: EntityId): EntityId {
        val auraId = putPermanentOnBattlefield(controller, aura.name)
        addComponent(auraId, AttachedToComponent(creature))
        addComponent(creature, AttachmentsComponent(listOf(auraId)))
        return auraId
    }

    test("the enchanted creature's controller has no maximum hand size") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.max(me) shouldBe 7
        d.enchant(me, bears)
        d.max(me) shouldBe null
        d.max(opp) shouldBe 7
    }

    test("an Aura on an opponent's creature frees the opponent, not the Aura's controller") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val theirs = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.enchant(me, theirs)
        d.max(opp) shouldBe null
        d.max(me) shouldBe 7
    }

    test("the grant ends when the Aura loses its abilities") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val auraId = d.enchant(me, bears)
        d.max(me) shouldBe null
        val result = d.services.effectExecutorRegistry.execute(
            d.state,
            Effects.RemoveAllAbilities(EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = auraId, controllerId = me, targets = listOf(ChosenTarget.Permanent(auraId)))
        )
        result.error shouldBe null
        d.replaceState(result.state)
        d.max(me) shouldBe 7
    }

    test("a printed no-maximum ability is also stripped by losing all abilities") {
        val d = driver(); val me = d.activePlayer!!
        val towerId = d.putPermanentOnBattlefield(me, tower.name)
        d.max(me) shouldBe null
        val result = d.services.effectExecutorRegistry.execute(
            d.state,
            Effects.RemoveAllAbilities(EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = towerId, controllerId = me, targets = listOf(ChosenTarget.Permanent(towerId)))
        )
        d.replaceState(result.state)
        d.max(me) shouldBe 7
    }

    test("a group grant reaches each creature its filter names") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, lord.name)
        d.max(me) shouldBe 7
        d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.max(opp) shouldBe 7
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.max(me) shouldBe null
    }

    test("a resolved 'gains no maximum hand size' grant counts") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val result = d.services.effectExecutorRegistry.execute(
            d.state,
            Effects.GrantStaticAbility(NoMaximumHandSize, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            EffectContext(sourceId = bears, controllerId = me, targets = listOf(ChosenTarget.Permanent(bears)))
        )
        result.error shouldBe null
        d.replaceState(result.state)
        d.max(me) shouldBe null
    }

    context("CR 613.7a: dated by the later of holder and granter") {
        test("an Aura attached after a set-maximum effect wins") {
            val d = driver(); val me = d.activePlayer!!
            val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
            d.stamp(bears, 1)
            d.stamp(d.putPermanentOnBattlefield(me, twenty.name), 5)
            d.stamp(d.enchant(me, bears), 10)
            d.max(me) shouldBe null
        }

        test("an older Aura on an older creature loses to a later set-maximum effect") {
            val d = driver(); val me = d.activePlayer!!
            val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
            d.stamp(bears, 1)
            d.stamp(d.enchant(me, bears), 3)
            d.stamp(d.putPermanentOnBattlefield(me, twenty.name), 5)
            d.max(me) shouldBe 20
        }

        test("an older Aura on a creature newer than the set-maximum effect wins") {
            val d = driver(); val me = d.activePlayer!!
            val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
            d.stamp(bears, 10)
            d.stamp(d.enchant(me, bears), 3)
            d.stamp(d.putPermanentOnBattlefield(me, twenty.name), 5)
            d.max(me) shouldBe null
        }
    }
})
