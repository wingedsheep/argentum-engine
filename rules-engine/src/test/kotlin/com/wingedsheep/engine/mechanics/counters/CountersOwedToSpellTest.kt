package com.wingedsheep.engine.mechanics.counters

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Counters put on a permanent spell while it's on the stack — "that creature enters with an
 * additional +1/+1 counter on it" (Runadi, Behemoth Caller; Jade Orb of Dragonkind) — are owed to the
 * permanent it becomes and given to it as it enters, not placed on the stack object.
 *
 * - CR 122.6: counters an object is given as it enters are counters "put on" it, so a placement
 *   replacement (Hardened Scales) applies to them then.
 * - CR 707.2: counters aren't copied, so a copy of the spell owes nothing.
 * - CR 400.7: a spell that never resolves carries nothing into its next zone.
 */
class CountersOwedToSpellTest : FunSpec({

    val patron = card("Owed Test Patron") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        }
    }
    val dynamicPatron = card("Owed Test Dynamic Patron") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.Creature)
            effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmount.Fixed(2), EffectTarget.TriggeringEntity)
        }
    }
    val scales = card("Owed Test Scales") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(ModifyCounterPlacement(modifier = 1))
    }
    val bear = card("Owed Test Bear") {
        manaCost = "{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
    }
    val cancel = card("Owed Test Cancel") {
        manaCost = "{U}"; typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.CounterSpell()
        }
    }
    val twin = card("Owed Test Twin") {
        manaCost = "{U}"; typeLine = "Instant"
        spell { effect = Effects.CopyTargetSpell(target(TargetFilter.CreatureSpellOnStack)) }
    }

    fun driver(vararg enchantments: String) = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(patron, dynamicPatron, scales, bear, cancel, twin))
        initMirrorMatch(deck = Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        enchantments.forEach { putPermanentOnBattlefield(player1, it) }
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast the Bear and resolve the cast trigger above it, leaving the Bear on the stack. */
    fun GameTestDriver.castBearAndResolveTrigger(): EntityId {
        giveMana(player1, Color.GREEN, 1)
        val bearId = putCardInHand(player1, bear.name)
        castSpell(player1, bearId).error shouldBe null
        stackSize shouldBe 2
        bothPass()
        stackSize shouldBe 1
        return bearId
    }

    test("the counter is owed to the spell and placed as the permanent enters") {
        val d = driver(patron.name)
        val bearId = d.castBearAndResolveTrigger()

        d.plusOnes(bearId) shouldBe 0
        val owed = d.state.getEntity(bearId)!!.get<SpellOnStackComponent>()!!.additionalEntryCounters
        owed.sumOf { it.count } shouldBe 1

        d.bothPass()
        d.findPermanent(d.player1, bear.name) shouldBe bearId
        d.plusOnes(bearId) shouldBe 1
    }

    test("a dynamic count is owed the same way") {
        val d = driver(dynamicPatron.name)
        val bearId = d.castBearAndResolveTrigger()
        d.plusOnes(bearId) shouldBe 0

        d.bothPass()
        d.plusOnes(bearId) shouldBe 2
    }

    test("a counter-placement replacement applies as it enters (CR 122.6)") {
        val d = driver(patron.name)
        val bearId = d.castBearAndResolveTrigger()
        // Scales arrives only after the counter was owed: it still modifies the entry placement.
        d.putPermanentOnBattlefield(d.player1, scales.name)

        d.bothPass()
        d.plusOnes(bearId) shouldBe 2
    }

    test("a countered spell carries no counters into the graveyard") {
        val d = driver(patron.name)
        val bearId = d.castBearAndResolveTrigger()

        d.giveMana(d.player1, Color.BLUE, 1)
        val cancelId = d.putCardInHand(d.player1, cancel.name)
        d.castSpellWithTargets(d.player1, cancelId, listOf(ChosenTarget.Spell(bearId))).error shouldBe null
        while (d.stackSize > 0) d.bothPass()

        d.state.getZone(d.player1, Zone.GRAVEYARD) shouldContain bearId
        d.plusOnes(bearId) shouldBe 0
    }

    test("a copy of the spell owes nothing (CR 707.2)") {
        val d = driver(patron.name)
        val bearId = d.castBearAndResolveTrigger()

        d.giveMana(d.player1, Color.BLUE, 1)
        val twinId = d.putCardInHand(d.player1, twin.name)
        d.castSpellWithTargets(d.player1, twinId, listOf(ChosenTarget.Spell(bearId))).error shouldBe null
        while (d.stackSize > 0) d.bothPass()

        val bears = d.getPermanents(d.player1).filter { d.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == bear.name }
        bears.size shouldBe 2
        val token = bears.single { d.state.getEntity(it)?.has<TokenComponent>() == true }
        d.plusOnes(token) shouldBe 0
        d.plusOnes(bearId) shouldBe 1
    }
})
