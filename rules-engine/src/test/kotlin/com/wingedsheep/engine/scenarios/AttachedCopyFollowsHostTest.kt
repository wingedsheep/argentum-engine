package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CopiableCharacteristicsChangedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.permanent.types.EachPermanentBecomesCopyOfTargetExecutor
import com.wingedsheep.engine.mechanics.sba.permanent.AttachedCopyExpiryCheck
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyWhileAttachedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Attachment-keyed copies: [Duration.WhileSourceAttached] (an Aura's own "enchanted creature is a
 * copy of …" static — CR 611.3a, it applies to whatever the Aura currently enchants) against
 * [Duration.WhileSourceAttachedToAffected] ("for as long as this remains attached to it" — CR 611.2b,
 * which ends for good once the source moves).
 */
class AttachedCopyFollowsHostTest : FunSpec({
    val aura = card("Follow Test Aura") { manaCost = "{U}"; typeLine = "Enchantment — Aura" }
    val model = card("Follow Test Model") { manaCost = "{3}"; typeLine = "Creature — Golem"; power = 5; toughness = 6 }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(aura, model))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun name(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!.name

    fun attach(d: GameTestDriver, attachment: EntityId, host: EntityId) =
        d.replaceState(d.state.updateEntity(attachment) { it.with(AttachedToComponent(host)) })

    /** The [attachment]'s host becomes a copy of [model], keyed to the attachment by [duration]. */
    fun copyHost(d: GameTestDriver, attachment: EntityId, modelId: EntityId, duration: Duration) {
        val result = EachPermanentBecomesCopyOfTargetExecutor(PredicateEvaluator(cardRegistry = d.cardRegistry), d.cardRegistry)
            .execute(d.state, EachPermanentBecomesCopyOfTargetEffect(
                target = EffectTarget.ContextTarget(0), affected = EffectTarget.EnchantedCreature, duration = duration),
                EffectContext(sourceId = attachment, controllerId = d.player1,
                    targets = listOf(ChosenTarget.Permanent(modelId))))
        d.replaceState(result.state)
    }

    fun sba(d: GameTestDriver): List<EntityId> {
        val result = AttachedCopyExpiryCheck(d.cardRegistry).check(d.state)
        d.replaceState(result.newState)
        return result.events.filterIsInstance<CopiableCharacteristicsChangedEvent>().map { it.entityId }
    }

    test("WhileSourceAttached: moving the source moves the locked-in copy to the new host") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, aura.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val modelId = d.putCreatureOnBattlefield(d.player2, model.name)
        attach(d, a, bears)
        copyHost(d, a, modelId, Duration.WhileSourceAttached)
        name(d, bears) shouldBe model.name
        sba(d) shouldBe emptyList()

        // The copy source leaving doesn't matter — the values were locked in.
        d.moveToGraveyard(modelId)
        attach(d, a, other)

        sba(d).shouldContainExactlyInAnyOrder(bears, other)
        name(d, bears) shouldBe "Grizzly Bears"
        name(d, other) shouldBe model.name
        d.state.projectedState.getPower(other) shouldBe 5
        d.state.getEntity(other)!!.get<CopyWhileAttachedComponent>()!!.attachmentId shouldBe a
        d.state.getEntity(bears)!!.has<CopyWhileAttachedComponent>() shouldBe false
    }

    test("WhileSourceAttached: the source becoming unattached or leaving ends the copy") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, aura.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val modelId = d.putCreatureOnBattlefield(d.player2, model.name)
        attach(d, a, bears)
        copyHost(d, a, modelId, Duration.WhileSourceAttached)

        d.moveToGraveyard(a)

        sba(d) shouldBe listOf(bears)
        name(d, bears) shouldBe "Grizzly Bears"
        d.state.projectedState.getPower(bears) shouldBe 2
    }

    test("WhileSourceAttachedToAffected: moving the source ends the copy and doesn't follow") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, aura.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val modelId = d.putCreatureOnBattlefield(d.player2, model.name)
        attach(d, a, bears)
        copyHost(d, a, modelId, Duration.WhileSourceAttachedToAffected)
        name(d, bears) shouldBe model.name

        attach(d, a, other)

        sba(d) shouldBe listOf(bears)
        name(d, bears) shouldBe "Grizzly Bears"
        name(d, other) shouldBe "Grizzly Bears"
    }

    test("WhileSourceAttached: a source not attached at resolution copies nothing") {
        val d = driver()
        val a = d.putPermanentOnBattlefield(d.player1, aura.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val modelId = d.putCreatureOnBattlefield(d.player2, model.name)

        copyHost(d, a, modelId, Duration.WhileSourceAttached)

        name(d, bears) shouldBe "Grizzly Bears"
    }
})
