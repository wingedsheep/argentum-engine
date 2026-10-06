package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j22.cards.MizzixReplicaRider
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mizzix, Replica Rider — "Whenever you cast a spell from anywhere other than your hand, you may
 * pay {1}{U/R}. If you do, copy that spell ... If the copy is a permanent spell, it gains haste and
 * 'At the beginning of your end step, sacrifice this permanent.'"
 */
class MizzixReplicaRiderScenarioTest : FunSpec({

    val graveyardCreature = card("Mizzix Test Graveyard Bear") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Bear"
        oracleText = "You may cast this card from your graveyard."
        power = 2
        toughness = 2
        staticAbility { ability = MayCastSelfFromZones(zones = listOf(Zone.GRAVEYARD)) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MizzixReplicaRider))
        driver.registerCard(graveyardCreature)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun bears(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId) =
        driver.getPermanents(player).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Mizzix Test Graveyard Bear"
        }

    test("casting a creature from the graveyard and paying copies it into a hasty token sacrificed at your end step") {
        val d = createDriver()
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(me, "Mizzix, Replica Rider")
        repeat(4) { d.putLandOnBattlefield(me, "Mountain") }
        val bear = d.putCardInGraveyard(me, "Mizzix Test Graveyard Bear")

        d.submit(CastSpell(playerId = me, cardId = bear, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null

        var paid = false
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 40) {
            when (d.pendingDecision) {
                is YesNoDecision -> { d.submitYesNo(me, true); paid = true }
                is SelectManaSourcesDecision -> d.submitManaAutoPayOrDecline(me, true)
                else -> d.bothPass()
            }
        }
        paid shouldBe true

        val all = bears(d, me)
        all.size shouldBe 2
        val token = all.single { d.state.getEntity(it)?.has<TokenComponent>() == true }
        d.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true

        d.passPriorityUntil(Step.END, maxPasses = 200)
        d.bothPass()
        bears(d, me).size shouldBe 1
    }

    test("declining the payment creates no copy") {
        val d = createDriver()
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(me, "Mizzix, Replica Rider")
        repeat(4) { d.putLandOnBattlefield(me, "Mountain") }
        val bear = d.putCardInGraveyard(me, "Mizzix Test Graveyard Bear")

        d.submit(CastSpell(playerId = me, cardId = bear, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null

        var asked = false
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 40) {
            when (d.pendingDecision) {
                is YesNoDecision -> { d.submitYesNo(me, false); asked = true }
                else -> d.bothPass()
            }
        }
        asked shouldBe true
        bears(d, me).size shouldBe 1
    }

    test("casting a spell from hand does not trigger") {
        val d = createDriver()
        val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(me, "Mizzix, Replica Rider")
        repeat(4) { d.putLandOnBattlefield(me, "Mountain") }
        val bear = d.putCardInHand(me, "Mizzix Test Graveyard Bear")

        d.submit(CastSpell(playerId = me, cardId = bear, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null

        var asked = false
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 40) {
            if (d.pendingDecision is YesNoDecision) asked = true
            d.bothPass()
        }
        asked shouldBe false
        bears(d, me).size shouldBe 1
    }
})
