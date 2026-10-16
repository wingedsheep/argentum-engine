package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Any player may tap an untapped creature they control" — a [Costs.pay.Tap] cost on
 * `AnyPlayerMayPayEffect` (Reservoir Kraken). Covers the first prompt (executor) and the
 * next-player prompt (continuation resumer): only the payer's untapped matching permanents are
 * offered, summoning sickness doesn't matter (no `{T}` symbol), paying taps the pick and runs the
 * consequence, and a player with nothing untapped is skipped.
 */
class AnyPlayerMayPayTapCostTest : FunSpec({

    /** "When this enters, any player may tap an untapped creature they control. If a player does, you gain 3 life." */
    val TapAny = card("Test Any-Player Tap") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.AnyPlayerMayPay(
                cost = Costs.pay.Tap(GameObjectFilter.Creature),
                consequence = Effects.GainLife(3)
            )
        }
    }

    val Bear = card("Test Bear") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCards(listOf(TapAny, Bear))
        d.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.resolveTapAny(playerId: EntityId) {
        val cardId = putCardInHand(playerId, "Test Any-Player Tap")
        giveMana(playerId, Color.GREEN, 1)
        castSpell(playerId, cardId)
        bothPass() // resolve the artifact
        bothPass() // resolve the ETB trigger
    }

    test("the first player is offered only their untapped creatures; paying taps the pick and runs the consequence") {
        val d = driver()
        val active = d.activePlayer!!
        val untapped = d.putCreatureOnBattlefield(active, "Test Bear") // summoning sick — still payable
        val tapped = d.putCreatureOnBattlefield(active, "Test Bear")
        d.tapPermanent(tapped)

        d.resolveTapAny(active)

        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe active
        decision.options shouldContainExactlyInAnyOrder listOf(untapped)
        decision.prompt shouldBe "You may tap an untapped creature you control. If you do, you gain 3 life."

        d.submitCardSelection(active, listOf(untapped))
        d.isTapped(untapped) shouldBe true
        d.getLifeTotal(active) shouldBe 23
        d.pendingDecision shouldBe null
    }

    test("a decline passes the offer to the next player, who pays with their own untapped creature") {
        val d = driver()
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        val activeBear = d.putCreatureOnBattlefield(active, "Test Bear")
        val oppBear = d.putCreatureOnBattlefield(opponent, "Test Bear")

        d.resolveTapAny(active)

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe active
        d.submitCardSelection(active, emptyList())

        val next = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        next.playerId shouldBe opponent
        next.options shouldContainExactlyInAnyOrder listOf(oppBear)

        d.submitCardSelection(opponent, listOf(oppBear))
        d.isTapped(oppBear) shouldBe true
        d.isTapped(activeBear) shouldBe false
        d.getLifeTotal(active) shouldBe 23
    }

    test("a player whose creatures are all tapped is skipped") {
        val d = driver()
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        val activeBear = d.putCreatureOnBattlefield(active, "Test Bear")
        d.tapPermanent(activeBear)
        val oppBear = d.putCreatureOnBattlefield(opponent, "Test Bear")

        d.resolveTapAny(active)

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe opponent
        d.submitCardSelection(opponent, emptyList())
        d.pendingDecision shouldBe null
        d.isTapped(oppBear) shouldBe false
        d.getLifeTotal(active) shouldBe 20
    }
})
