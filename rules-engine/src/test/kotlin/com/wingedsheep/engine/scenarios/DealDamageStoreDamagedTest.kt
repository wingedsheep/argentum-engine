package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.RedirectDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * `DealDamageEffect.storeDamagedAs` — "each player / creature dealt damage this way": the recipients
 * of at least 1 damage from the instruction, read off the committed damage events, so a wholly
 * prevented recipient is left out and a redirected one is counted where the damage actually landed.
 * The multi-recipient forms stay one simultaneous damage event.
 */
class DealDamageStoreDamagedTest : FunSpec({
    val shield = card("Store Test Shield") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(PreventDamage(appliesTo = EventPattern.DamageEvent(recipient = Recipient.You)))
    }
    val mirror = card("Store Test Mirror") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(
            RedirectDamage(
                redirectTo = EffectTarget.ControllerOfDamageSource,
                appliesTo = EventPattern.DamageEvent(recipient = Recipient.You),
            )
        )
    }
    val eachOpponent = EffectTarget.PlayerRef(Player.EachOpponent)

    fun twoPlayer() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(shield, mirror))
        initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("each opponent dealt damage is stored; a fully prevented opponent is not") {
        val d = GameTestDriver().apply { registerCards(TestCards.all + listOf(shield)) }
        val (me, hit, shielded) = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) }, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(shielded, shield.name)
        val source = d.putCreatureOnBattlefield(me, "Grizzly Bears")

        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.DealDamage(6, eachOpponent, storeDamagedAs = "hit"), EffectContext(source, me))

        result.error shouldBe null
        result.updatedCollections["hit"] shouldBe listOf(hit)
        result.events.filterIsInstance<DamageDealtEvent>().filter { it.amount > 0 }.map { it.targetId } shouldBe listOf(hit)
        d.replaceState(result.state)
        d.getLifeTotal(hit) shouldBe 14
        d.getLifeTotal(shielded) shouldBe 20
    }

    test("damage redirected to the source's controller stores that player, not the original recipient") {
        val d = twoPlayer()
        d.putPermanentOnBattlefield(d.player2, mirror.name)
        val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.DealDamage(4, eachOpponent, storeDamagedAs = "hit"), EffectContext(source, d.player1))

        result.error shouldBe null
        result.updatedCollections["hit"] shouldBe listOf(d.player1)
        d.replaceState(result.state)
        d.getLifeTotal(d.player1) shouldBe 16
        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("no damage dealt still writes an empty collection, so a reader sees none rather than a missing slot") {
        val d = twoPlayer()
        val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.DealDamage(0, eachOpponent, storeDamagedAs = "hit"), EffectContext(source, d.player1))

        result.error shouldBe null
        result.updatedCollections["hit"] shouldBe emptyList()
    }

    test("an atomic group loop stores every creature it dealt damage to") {
        val d = twoPlayer()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val source = d.putPermanentOnBattlefield(d.player1, shield.name)

        val result = d.services.effectExecutorRegistry.execute(d.state,
            Effects.ForEachInGroup(GroupFilter.AllCreatures,
                Effects.DealDamage(1, EffectTarget.IterationEntity, storeDamagedAs = "hit")),
            EffectContext(source, d.player1))

        result.error shouldBe null
        result.updatedCollections["hit"]!!.shouldContainExactlyInAnyOrder(bear, giant)
    }
})
