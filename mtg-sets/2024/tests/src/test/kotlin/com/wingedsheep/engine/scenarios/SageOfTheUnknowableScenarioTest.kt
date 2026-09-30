package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.SageOfTheUnknowable
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sage of the Unknowable — "{T}: Add {C}. Spend this mana only to cast a colorless spell or to
 * activate an ability."
 */
class SageOfTheUnknowableScenarioTest : FunSpec({

    val colorlessTrinket = CardDefinition.artifact(name = "Sage Colorless Trinket", manaCost = ManaCost.parse("{1}"))
    val blueTrinket = CardDefinition.artifact(name = "Sage Blue Trinket", manaCost = ManaCost.parse("{1}{U}"))
    val sageCurio = card("Sage Curio") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "{1}: You gain 3 life."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(3)
            timing = TimingRule.InstantSpeed
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SageOfTheUnknowable, colorlessTrinket, blueTrinket, sageCurio))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.tapSage(playerId: EntityId) {
        val sage = putCreatureOnBattlefield(playerId, "Sage of the Unknowable")
        removeSummoningSickness(sage)
        submitSuccess(ActivateAbility(playerId, sage, SageOfTheUnknowable.activatedAbilities.single().id))
        state.getEntity(playerId)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 1
    }

    test("the mana pays for a colorless spell") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.tapSage(me)
        val trinket = driver.putCardInHand(me, "Sage Colorless Trinket")

        val result = driver.submit(CastSpell(playerId = me, cardId = trinket, paymentStrategy = PaymentStrategy.FromPool))
        result.outcome shouldBe Outcome.Done
        driver.state.getEntity(me)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 0
    }

    test("the mana cannot pay for a colored spell's generic cost") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.tapSage(me)
        driver.giveMana(me, Color.BLUE, 1)
        val trinket = driver.putCardInHand(me, "Sage Blue Trinket")

        val result = driver.submit(CastSpell(playerId = me, cardId = trinket, paymentStrategy = PaymentStrategy.FromPool))
        result.outcome shouldNotBe Outcome.Done
    }

    test("the mana pays for any activated ability") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val curio = driver.putPermanentOnBattlefield(me, "Sage Curio")
        driver.tapSage(me)
        val lifeBefore = driver.getLifeTotal(me)

        driver.submitSuccess(ActivateAbility(me, curio, sageCurio.activatedAbilities.single().id))
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe lifeBefore + 3
    }
})
