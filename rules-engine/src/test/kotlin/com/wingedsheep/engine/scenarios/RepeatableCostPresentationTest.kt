package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mechanics.squad
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RepeatableCostPresentationTest : FunSpec({
    val compositeSquad = card("Test Composite Squad") {
        manaCost = "{R}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        squad("{1}", AdditionalCost.Composite(listOf(
            Costs.additional.DiscardCards(),
            AdditionalCost.Composite(listOf(Costs.additional.ExileCards(2))),
            Costs.additional.PayLife(3),
        )))
        staticAbility { ability = MayCastSelfFromZones(zones = listOf(Zone.GRAVEYARD)) }
    }
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + compositeSquad)
        it.initMirrorMatch(Deck.of("Mountain" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = it.activePlayer!!
        it.getHand(player).forEach(it::moveToGraveyard)
        repeat(5) { _ -> it.putLandOnBattlefield(player, "Mountain") }
    }

    listOf(Zone.HAND, Zone.GRAVEYARD).forEach { zone ->
        test("a repeated composite from $zone advertises both scaled pickers and can be paid") {
            val d = setup()
            val player = d.activePlayer!!
            val source = if (zone == Zone.HAND) d.putCardInHand(player, compositeSquad.name)
                else d.putCardInGraveyard(player, compositeSquad.name)
            val discards = List(2) { d.putCardInHand(player, "Mountain") }
            val exiles = d.getGraveyard(player).filter { it != source }.take(4)
            val actions = LegalActionEnumerator.create(d.cardRegistry).enumerate(d.state, player)
                .filter { (it.action as? CastSpell)?.let { cast ->
                    cast.cardId == source && cast.declaredCostSlot == ChoiceSlot.SQUAD
                } == true }
            actions.map { (it.action as CastSpell).declaredCostTimes } shouldBe listOf(1, 2)
            val twice = actions.last()
            twice.affordable shouldBe true
            val discardInfo = twice.additionalCostInfo!!
            discardInfo.costType shouldBe "DiscardCard"
            discardInfo.discardCount shouldBe 2
            val exileInfo = discardInfo.alsoRequired.single()
            exileInfo.costType shouldBe "ExileFromGraveyard"
            exileInfo.exileMinCount shouldBe 4
            exileInfo.exileMaxCount shouldBe 4
            d.submit((twice.action as CastSpell).copy(
                additionalCostPayment = AdditionalCostPayment(discardedCards = discards, exiledCards = exiles)
            )).outcome shouldBe Outcome.Done
            d.getExile(player).toSet() shouldBe exiles.toSet()
            d.getLifeTotal(player) shouldBe 14
            d.bothPass()
            d.bothPass()
            d.getPermanents(player).count { d.getCardName(it) == compositeSquad.name } shouldBe 3
        }
        test("repeated composite $zone counts are capped by life affordability") {
            val d = setup()
            val player = d.activePlayer!!
            d.setLifeTotal(player, 5)
            val source = if (zone == Zone.HAND) d.putCardInHand(player, compositeSquad.name)
                else d.putCardInGraveyard(player, compositeSquad.name)
            repeat(3) { d.putCardInHand(player, "Mountain") }
            val actions = LegalActionEnumerator.create(d.cardRegistry).enumerate(d.state, player)
                .filter { (it.action as? CastSpell)?.let { cast ->
                    cast.cardId == source && cast.declaredCostSlot == ChoiceSlot.SQUAD
                } == true }
            actions.map { (it.action as CastSpell).declaredCostTimes } shouldBe listOf(1)
            actions.single().affordable shouldBe true
        }
    }
})
