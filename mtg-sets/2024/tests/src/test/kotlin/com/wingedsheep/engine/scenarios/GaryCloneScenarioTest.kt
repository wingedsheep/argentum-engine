package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.mtg.sets.definitions.pip.cards.GaryClone

class GaryCloneScenarioTest : FunSpec({
    val blink = com.wingedsheep.sdk.dsl.card("Test Squad Blink") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val creature = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.CreatureYouControl)
            effect = com.wingedsheep.sdk.dsl.Effects.Exile(creature) then
                com.wingedsheep.sdk.dsl.Effects.PutOntoBattlefieldUnderYourControl(creature)
        }
    }
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(GaryClone, blink))
        it.initMirrorMatch(Deck.of("Plains" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("two squad payments create two copies without recursive squad triggers") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(6) { d.putLandOnBattlefield(player, "Plains") }
        val source = d.putCardInHand(player, "Gary Clone")
        d.submit(CastSpell(player, source, declaredCostSlot = ChoiceSlot.SQUAD, declaredCostTimes = 2)).outcome shouldBe Outcome.Done
        d.bothPass() // Creature resolves; one squad trigger.
        d.state.stack.size shouldBe 1
        d.state.getEntity(source)!!.get<CastChoicesComponent>()!!.chosen[ChoiceSlot.SQUAD] shouldBe ChoiceValue.NumberChoice(2)
        d.state.getEntity(source)!!.get<CastChoicesComponent>()!!.chosen.containsKey(ChoiceSlot.KICKED) shouldBe false
        d.bothPass()
        val copies = d.getPermanents(player).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
        copies.size shouldBe 2
        copies.all { d.state.getEntity(it)?.get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.SQUAD) != true } shouldBe true
        d.state.stack.size shouldBe 0
        d.getPermanents(player).filter { d.getCardName(it) == "Plains" }.all { d.isTapped(it) } shouldBe true
    }

    test("no squad payment and putting directly onto battlefield produce no copies") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(2) { d.putLandOnBattlefield(player, "Plains") }
        val source = d.putCardInHand(player, "Gary Clone")
        d.castSpell(player, source).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.stack.size shouldBe 0
        d.putPermanentOnBattlefield(player, "Gary Clone")
        d.getPermanents(player).count { d.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 0
    }

    test("removing the source before its squad trigger resolves preserves the payment count") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(6) { d.putLandOnBattlefield(player, "Plains") }
        val source = d.putCardInHand(player, "Gary Clone")
        d.submit(CastSpell(player, source, declaredCostSlot = ChoiceSlot.SQUAD, declaredCostTimes = 2)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.giveMana(player, Color.RED)
        val bolt = d.putCardInHand(player, "Lightning Bolt")
        d.castSpell(player, bolt, listOf(source)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.getEntity(source)?.get<CastChoicesComponent>() shouldBe null
        d.bothPass()
        d.getPermanents(player).count { d.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 2
    }

    test("blinking the source does not reset the old trigger or copy a new object's payment choices") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(6) { d.putLandOnBattlefield(player, "Plains") }
        val source = d.putCardInHand(player, "Gary Clone")
        d.submit(CastSpell(player, source, declaredCostSlot = ChoiceSlot.SQUAD, declaredCostTimes = 2)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.giveMana(player, Color.WHITE)
        val spell = d.putCardInHand(player, blink.name)
        d.castSpell(player, spell, listOf(source)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.getEntity(source)?.get<CastChoicesComponent>() shouldBe null
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getPermanents(player).count { d.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 2
        d.state.stack.size shouldBe 0
    }

    test("countering the creature spell prevents squad's entry trigger") {
        val d = setup()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        repeat(4) { d.putLandOnBattlefield(player, "Plains") }
        val source = d.putCardInHand(player, "Gary Clone")
        d.submit(CastSpell(player, source, declaredCostSlot = ChoiceSlot.SQUAD)).outcome shouldBe Outcome.Done
        d.passPriority(player)
        d.giveMana(opponent, Color.BLUE, 2)
        val counter = d.putCardInHand(opponent, "Counterspell")
        d.submit(CastSpell(opponent, counter, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(source)),
            paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.stack.size shouldBe 0
        d.getPermanents(player).count { d.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 0
    }

    test("each attacking Gary buffs only the Gary creatures its controller controls") {
        val d = setup()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val first = d.putCreatureOnBattlefield(player, "Gary Clone")
        val second = d.putCreatureOnBattlefield(player, "Gary Clone")
        val other = d.putCreatureOnBattlefield(player, "Grizzly Bears")
        val enemy = d.putCreatureOnBattlefield(opponent, "Gary Clone")
        d.removeSummoningSickness(first)
        d.removeSummoningSickness(second)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(player, listOf(first, second), opponent).outcome shouldBe Outcome.Done
        d.bothPass()
        d.bothPass()
        d.state.projectedState.getPower(first) shouldBe 3
        d.state.projectedState.getPower(second) shouldBe 3
        d.state.projectedState.getPower(other) shouldBe 2
        d.state.projectedState.getPower(enemy) shouldBe 1
    }
})
