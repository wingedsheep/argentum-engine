package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.dsl.mechanics.squad
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RepeatableAdditionalCostTest : FunSpec({
    val discardSquad = card("Test Discard Squad") {
        manaCost = "{R}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        squad("{1}", Costs.additional.DiscardCards())
        staticAbility { ability = MayCastSelfFromZones(zones = listOf(com.wingedsheep.sdk.core.Zone.GRAVEYARD)) }
    }
    val multikicker = card("Test Counted Multikicker") {
        manaCost = "{R}"
        typeLine = "Enchantment"
        keywordAbility(KeywordAbility.multikicker("{1}"))
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.DrawCards(DynamicAmount.CastChoice(ChoiceSlot.KICKED))
        }
    }
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(discardSquad, multikicker))
        it.initMirrorMatch(Deck.of("Mountain" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a mixed mana and discard cost charges each half per payment") {
        val d = setup()
        val player = d.activePlayer!!
        d.giveMana(player, Color.RED)
        d.giveColorlessMana(player, 2)
        val source = d.putCardInHand(player, discardSquad.name)
        val cards = List(2) { d.putCardInHand(player, "Mountain") }
        d.submit(CastSpell(player, source, paymentStrategy = PaymentStrategy.FromPool,
            declaredCostSlot = ChoiceSlot.SQUAD, declaredCostTimes = 2,
            additionalCostPayment = AdditionalCostPayment(discardedCards = cards))).outcome shouldBe Outcome.Done
        d.getGraveyard(player).size shouldBe 2
        d.state.getEntity(player)!!.get<ManaPoolComponent>()!!.total shouldBe 0
        d.bothPass()
        d.bothPass()
        d.getPermanents(player).count { d.getCardName(it) == discardSquad.name } shouldBe 3
    }

    test("two payments cannot discard the same card twice or underpay mana") {
        val d = setup()
        val player = d.activePlayer!!
        d.giveMana(player, Color.RED)
        d.giveColorlessMana(player, 1)
        val source = d.putCardInHand(player, discardSquad.name)
        val cards = List(2) { d.putCardInHand(player, "Mountain") }
        val action = CastSpell(player, source, paymentStrategy = PaymentStrategy.FromPool,
            declaredCostSlot = ChoiceSlot.SQUAD, declaredCostTimes = 2,
            additionalCostPayment = AdditionalCostPayment(discardedCards = cards))
        d.submit(action).outcome shouldNotBe Outcome.Done
        d.getGraveyard(player).size shouldBe 0
        d.giveColorlessMana(player, 1)
        d.submit(action.copy(additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(cards.first(), cards.first()))))
            .outcome shouldNotBe Outcome.Done
        d.getGraveyard(player).size shouldBe 0
    }

    test("the discard picker and affordable count agree with casting") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(4) { d.putLandOnBattlefield(player, "Mountain") }
        d.putCardInHand(player, discardSquad.name)
        repeat(2) { d.putCardInHand(player, "Mountain") }
        // Empty the initial opening hand so it cannot supply extra discard payments.
        val source = d.getHand(player).first { d.getCardName(it) == discardSquad.name }
        val keep = d.getHand(player).takeLast(2).toSet() + source
        d.getHand(player).filter { it !in keep }.forEach(d::moveToGraveyard)
        val actions = LegalActionEnumerator.create(d.cardRegistry).enumerate(d.state, player)
            .filter { (it.action as? CastSpell)?.declaredCostSlot == ChoiceSlot.SQUAD }
        actions.map { (it.action as CastSpell).declaredCostTimes } shouldBe listOf(1, 2)
        actions.all { it.affordable } shouldBe true
    }

    test("non-hand squad offers and charges repeated discard payments") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(3) { d.putLandOnBattlefield(player, "Mountain") }
        val source = d.putCardInGraveyard(player, discardSquad.name)
        val cards = List(2) { d.putCardInHand(player, "Mountain") }
        val actions = LegalActionEnumerator.create(d.cardRegistry).enumerate(d.state, player)
        val cast = actions.mapNotNull { it.action as? CastSpell }.first {
            it.cardId == source && it.declaredCostSlot == ChoiceSlot.SQUAD && it.declaredCostTimes == 2
        }
        d.submit(cast.copy(additionalCostPayment = AdditionalCostPayment(discardedCards = cards))).outcome shouldBe Outcome.Done
        d.bothPass()
        d.bothPass()
        d.getPermanents(player).count { d.getCardName(it) == discardSquad.name } shouldBe 3
    }

    test("the menu does not truncate affordable repeat counts at ten") {
        val d = setup()
        val player = d.activePlayer!!
        d.giveMana(player, Color.RED)
        d.giveColorlessMana(player, 12)
        val source = d.putCardInHand(player, multikicker.name)
        val times = LegalActionEnumerator.create(d.cardRegistry).enumerate(d.state, player)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == source && it.declaredCostSlot == ChoiceSlot.KICKED }
            .map { it.declaredCostTimes }
        times shouldBe (1..12).toList()
    }

    test("an oversized declaration is rejected before allocating its repeated mana symbols") {
        val d = setup()
        val player = d.activePlayer!!
        val source = d.putCardInHand(player, multikicker.name)
        d.submit(CastSpell(player, source, declaredCostSlot = ChoiceSlot.KICKED,
            declaredCostTimes = Int.MAX_VALUE)).outcome shouldNotBe Outcome.Done
        d.state.stack.size shouldBe 0
    }

    test("multikicker count survives resolution and source removal") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(4) { d.putLandOnBattlefield(player, "Mountain") }
        val source = d.putCardInHand(player, multikicker.name)
        val before = d.getHand(player).size
        d.submit(CastSpell(player, source, declaredCostSlot = ChoiceSlot.KICKED, declaredCostTimes = 3)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.moveToGraveyard(source)
        d.replaceState(d.state.updateEntity(source) { it.without<com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent>() })
        d.bothPass()
        d.getHand(player).size shouldBe before - 1 + 3
    }
})
