package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A static `GrantStaticAbility` whose granted ability generates a continuous effect of its own —
 * a Kaldheim Rune's "As long as enchanted permanent is an Equipment, it has 'Equipped creature gets
 * +1/+1 and has trample'". The holder (the Equipment) generates that effect as if it were printed
 * on it, so it reaches whichever creature the Equipment is attached to, and it stops when the
 * granter or the holder loses its abilities, or the grant's condition fails.
 */
class GrantedLoweredStaticTest : FunSpec({
    val pumpGrant = CompositeStaticAbility(listOf(ModifyStats(1, 1), GrantKeyword(Keyword.TRAMPLE)))
    val equipment = card("Test Plain Equipment") { typeLine = "Artifact — Equipment" }
    val trinket = card("Test Plain Trinket") { typeLine = "Artifact" }
    val rune = card("Test Equipment Rune") {
        typeLine = "Enchantment — Aura"
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = GrantStaticAbility(pumpGrant),
                condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)),
            )
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(equipment, trinket, rune))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.attach(attachment: EntityId, host: EntityId) {
        var next = state
        next.getEntity(attachment)?.get<AttachedToComponent>()?.targetId?.let { old ->
            next = next.updateEntity(old) { c ->
                c.with(AttachmentsComponent((c.get<AttachmentsComponent>()?.attachedIds ?: emptyList()) - attachment))
            }
        }
        next = next.updateEntity(attachment) { it.with(AttachedToComponent(host)) }
        next = next.updateEntity(host) { c ->
            c.with(AttachmentsComponent((c.get<AttachmentsComponent>()?.attachedIds ?: emptyList()) + attachment))
        }
        replaceState(next)
    }

    /** A resolved "loses all abilities" from a third permanent (a self-sourced one would exempt its target). */
    fun GameTestDriver.loseAllAbilities(target: EntityId) {
        val caster = putPermanentOnBattlefield(activePlayer!!, trinket.name)
        val result = services.effectExecutorRegistry.execute(
            state,
            Effects.RemoveAllAbilities(EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = caster, controllerId = activePlayer!!, targets = listOf(ChosenTarget.Permanent(target)))
        )
        result.error shouldBe null
        replaceState(result.state)
    }

    fun GameTestDriver.isPumped(creature: EntityId): Boolean {
        val projected = state.projectedState
        return projected.getPower(creature) == 3 && projected.getToughness(creature) == 3 &&
            projected.hasKeyword(creature, Keyword.TRAMPLE)
    }

    test("the Equipment generates the granted effect on the creature it's attached to") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val gear = d.putPermanentOnBattlefield(me, equipment.name)
        val aura = d.putPermanentOnBattlefield(me, rune.name)
        d.attach(gear, bears)
        d.attach(aura, gear)

        d.isPumped(bears) shouldBe true
        d.state.projectedState.getPower(other) shouldBe 2
        d.state.projectedState.hasKeyword(gear, Keyword.TRAMPLE) shouldBe false
    }

    test("re-equipping moves the bonus; an unattached Equipment pumps nobody") {
        val d = driver(); val me = d.activePlayer!!
        val first = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val second = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val gear = d.putPermanentOnBattlefield(me, equipment.name)
        val aura = d.putPermanentOnBattlefield(me, rune.name)
        d.attach(aura, gear)

        d.isPumped(first) shouldBe false
        d.isPumped(second) shouldBe false

        d.attach(gear, first)
        d.isPumped(first) shouldBe true

        d.attach(gear, second)
        d.isPumped(first) shouldBe false
        d.isPumped(second) shouldBe true
    }

    test("the grant's condition gates it: a non-Equipment artifact gets nothing to hand out") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val thing = d.putPermanentOnBattlefield(me, trinket.name)
        val aura = d.putPermanentOnBattlefield(me, rune.name)
        d.attach(thing, bears)
        d.attach(aura, thing)

        d.isPumped(bears) shouldBe false
    }

    test("the effect ends when the granter loses its abilities") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val gear = d.putPermanentOnBattlefield(me, equipment.name)
        val aura = d.putPermanentOnBattlefield(me, rune.name)
        d.attach(gear, bears)
        d.attach(aura, gear)
        d.isPumped(bears) shouldBe true

        d.loseAllAbilities(aura)
        // Both halves end — the granted keyword too, though the removal is newer than the grant.
        d.state.projectedState.getPower(bears) shouldBe 2
        d.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
    }

    test("the effect ends when the holder loses its abilities") {
        val d = driver(); val me = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val gear = d.putPermanentOnBattlefield(me, equipment.name)
        val aura = d.putPermanentOnBattlefield(me, rune.name)
        d.attach(gear, bears)
        d.attach(aura, gear)

        d.loseAllAbilities(gear)
        d.state.projectedState.getPower(bears) shouldBe 2
        d.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
    }

    test("granting a static that neither projects nor lowers fails loudly") {
        shouldThrow<IllegalStateException> {
            StaticAbilityHandler.lower(listOf(GrantStaticAbility(CantBeBlockedByMoreThan(1))))
        }
    }
})
