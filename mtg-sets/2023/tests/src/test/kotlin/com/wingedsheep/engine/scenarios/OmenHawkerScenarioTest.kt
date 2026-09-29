package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.OmenHawker
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
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Omen Hawker — "{T}: Add {C}{U}. Spend this mana only to activate abilities."
 */
class OmenHawkerScenarioTest : FunSpec({

    // {C}{U}: You gain 3 life — only payable if the Hawker's colorless AND blue halves both land.
    val hawkerCurio = card("Hawker Curio") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "{C}{U}: You gain 3 life."
        activatedAbility {
            cost = Costs.Mana("{C}{U}")
            effect = Effects.GainLife(3)
            timing = TimingRule.InstantSpeed
        }
    }
    val hawkerTrinket = CardDefinition.artifact(name = "Hawker Trinket", manaCost = ManaCost.parse("{1}{U}"))

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OmenHawker, hawkerCurio, hawkerTrinket))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val manaAbilityId = OmenHawker.activatedAbilities.single().id

    fun GameTestDriver.tapHawker(playerId: EntityId) {
        val hawker = putCreatureOnBattlefield(playerId, "Omen Hawker")
        removeSummoningSickness(hawker)
        submitSuccess(ActivateAbility(playerId, hawker, manaAbilityId))
    }

    test("tapping adds {C}{U}, both restricted to ability activation") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.tapHawker(me)

        val pool = driver.state.getEntity(me)!!.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 2
        pool.restrictedMana.map { it.color }.toSet() shouldBe setOf(null, Color.BLUE)
        pool.restrictedMana.all { it.restriction == ManaRestriction.AbilityActivationOnly } shouldBe true
        pool.colorless shouldBe 0
        pool.blue shouldBe 0
    }

    test("the mana pays for an activated ability") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val curio = driver.putPermanentOnBattlefield(me, "Hawker Curio")
        driver.tapHawker(me)
        val lifeBefore = driver.getLifeTotal(me)

        val abilityId = hawkerCurio.activatedAbilities.single().id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = curio, abilityId = abilityId))
        driver.state.getEntity(me)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 0
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe lifeBefore + 3
    }

    test("the mana cannot pay for a spell") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.tapHawker(me)

        val trinket = driver.putCardInHand(me, "Hawker Trinket")
        val result = driver.submit(CastSpell(playerId = me, cardId = trinket, paymentStrategy = PaymentStrategy.FromPool))
        result.outcome shouldNotBe Outcome.Done
    }
})
