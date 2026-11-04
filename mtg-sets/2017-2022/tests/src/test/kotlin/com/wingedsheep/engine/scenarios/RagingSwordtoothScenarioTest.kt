package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.RagingSwordtooth
import com.wingedsheep.mtg.sets.definitions.xln.cards.RaptorHatchling
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RagingSwordtoothScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(RagingSwordtooth, RaptorHatchling))
        initMirrorMatch(Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun cast(d: GameTestDriver): EntityId {
        val player = d.activePlayer!!
        val swordtooth = d.putCardInHand(player, "Raging Swordtooth")
        d.giveMana(player, Color.GREEN, 4)
        d.giveMana(player, Color.RED, 1)
        d.castSpell(player, swordtooth).error shouldBe null
        d.bothPass().error shouldBe null
        return swordtooth
    }

    test("damages all other creatures before deaths and triggers enrage") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val hatchling = d.putCreatureOnBattlefield(player, "Raptor Hatchling")
        val bears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val swordtooth = cast(d)
        val events = d.bothPass().events
        val damage = events.filterIsInstance<DamageDealtEvent>()
        damage.map { it.targetId }.toSet() shouldBe setOf(hatchling, bears)
        damage.map { it.amount } shouldBe listOf(1, 1)
        val deathIndex = events.indexOfFirst {
            it is ZoneChangeEvent && it.entityId == hatchling && it.toZone == Zone.GRAVEYARD
        }
        (deathIndex > events.indexOfLast { it is DamageDealtEvent }) shouldBe true
        (d.state.getEntity(swordtooth)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        d.state.projectedState.hasKeyword(swordtooth, Keyword.TRAMPLE) shouldBe true
        d.state.getEntity(bears)?.get<DamageComponent>()?.amount shouldBe 1
        d.bothPass().error shouldBe null
        (d.findPermanent(player, "Dinosaur Token") != null) shouldBe true
    }

    test("the triggered damage still happens after Swordtooth leaves") {
        val d = driver()
        val player = d.activePlayer!!
        val bears = d.putCreatureOnBattlefield(d.getOpponent(player), "Grizzly Bears")
        val swordtooth = cast(d)
        d.moveToGraveyard(swordtooth)
        d.bothPass().error shouldBe null
        d.state.getEntity(bears)?.get<DamageComponent>()?.amount shouldBe 1
    }

    test("no other creatures means no damage or decisions") {
        val d = driver()
        val swordtooth = cast(d)
        val result = d.bothPass()
        result.error shouldBe null
        result.events.filterIsInstance<DamageDealtEvent>() shouldBe emptyList()
        d.pendingDecision shouldBe null
        (d.state.getEntity(swordtooth)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }
})
