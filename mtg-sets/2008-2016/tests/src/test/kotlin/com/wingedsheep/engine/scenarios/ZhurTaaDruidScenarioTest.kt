package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.ZhurTaaDruid
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Zhur-Taa Druid — "{T}: Add {G}. Whenever you tap this creature for mana, it deals 1 damage to each
 * opponent."
 *
 * Proves `Triggers.self.tappedForMana()` fires for a non-land source: the rider goes on the stack after
 * a manual activation and after the auto-payer taps the Druid for a spell.
 */
class ZhurTaaDruidScenarioTest : FunSpec({

    val manaAbilityId = ZhurTaaDruid.activatedAbilities.single().id

    val TestBear = card("Druid Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ZhurTaaDruid, TestBear))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
    }

    test("tapping it for mana adds {G} and deals 1 damage to each opponent") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val druid = d.putCreatureOnBattlefield(you, "Zhur-Taa Druid")
        d.removeSummoningSickness(druid)

        d.submit(ActivateAbility(playerId = you, sourceId = druid, abilityId = manaAbilityId)).error shouldBe null

        d.state.getEntity(you)!!.get<ManaPoolComponent>()!!.green shouldBe 1
        d.stackSize shouldBe 1
        d.bothPass()

        d.getLifeTotal(opponent) shouldBe 19
        d.getLifeTotal(you) shouldBe 20
    }

    test("the auto-payer tapping it for a spell fires the trigger too") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val druid = d.putCreatureOnBattlefield(you, "Zhur-Taa Druid")
        d.removeSummoningSickness(druid)

        val bear = d.putCardInHand(you, "Druid Test Bear")
        d.submit(CastSpell(playerId = you, cardId = bear, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null

        d.isTapped(druid) shouldBe true
        d.stackSize shouldBe 2
        d.bothPass()
        d.getLifeTotal(opponent) shouldBe 19
    }
})
