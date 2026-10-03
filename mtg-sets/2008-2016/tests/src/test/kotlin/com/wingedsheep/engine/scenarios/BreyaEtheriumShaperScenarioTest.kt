package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c16.cards.BreyaEtheriumShaper
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Breya, Etherium Shaper — {W}{U}{B}{R} Legendary Artifact Creature — Human 4/4 (C16 #29).
 *
 * "When Breya enters, create two 1/1 blue Thopter artifact creature tokens with flying.
 *  {2}, Sacrifice two artifacts: Choose one —
 *  • Breya deals 3 damage to target player or planeswalker.
 *  • Target creature gets -4/-4 until end of turn.
 *  • You gain 5 life."
 */
class BreyaEtheriumShaperScenarioTest : FunSpec({

    val abilityId = BreyaEtheriumShaper.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BreyaEtheriumShaper))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.thoptersOf(playerId: EntityId): List<EntityId> =
        state.getZone(playerId, Zone.BATTLEFIELD).filter {
            state.getEntity(it)?.get<CardComponent>()?.typeLine?.subtypes?.any { s -> s.value == "Thopter" } == true
        }

    /** Cast Breya from hand and resolve her and her enters trigger. */
    fun GameTestDriver.castBreya(me: EntityId): EntityId {
        val card = putCardInHand(me, "Breya, Etherium Shaper")
        giveMana(me, Color.WHITE)
        giveMana(me, Color.BLUE)
        giveMana(me, Color.BLACK)
        giveMana(me, Color.RED)
        castSpell(me, card).outcome shouldBe Outcome.Done
        bothPass() // Breya resolves → trigger
        bothPass() // trigger resolves
        return findPermanent(me, "Breya, Etherium Shaper")!!
    }

    fun GameTestDriver.activate(me: EntityId, breya: EntityId, sacrifice: List<EntityId>, mode: Int) {
        giveColorlessMana(me, 2)
        submit(
            ActivateAbility(
                playerId = me,
                sourceId = breya,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = sacrifice)
            )
        ).outcome shouldBe Outcome.Done
        bothPass() // resolve → mode choice
        val decision = pendingDecision as ChooseOptionDecision
        submitDecision(me, OptionChosenResponse(decision.id, mode))
    }

    test("enters: creates two 1/1 blue flying Thopter artifact creature tokens") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.castBreya(me)

        val thopters = driver.thoptersOf(me)
        thopters.size shouldBe 2
        thopters.forEach { id ->
            val projected = driver.state.projectedState
            projected.getPower(id) shouldBe 1
            projected.getToughness(id) shouldBe 1
            projected.isCreature(id) shouldBe true
            projected.hasKeyword(id, Keyword.FLYING) shouldBe true
            projected.getColors(id) shouldBe setOf(Color.BLUE.name)
            driver.state.getEntity(id)!!.get<CardComponent>()!!.typeLine.isArtifact shouldBe true
        }
    }

    test("sacrifice two Thopters: target creature gets -4/-4 until end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val breya = driver.castBreya(me)
        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        driver.activate(me, breya, driver.thoptersOf(me), mode = 1)
        driver.submitTargetSelection(me, listOf(courser))
        driver.bothPass()

        driver.thoptersOf(me).size shouldBe 0
        driver.findPermanent(opp, "Centaur Courser") shouldBe null
        driver.getGraveyardCardNames(opp) shouldContain "Centaur Courser"
    }

    test("sacrifice Breya herself and a Thopter: Breya deals 3 damage to target player") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val breya = driver.castBreya(me)

        driver.activate(me, breya, listOf(breya, driver.thoptersOf(me).first()), mode = 0)
        driver.submitTargetSelection(me, listOf(opp))
        driver.bothPass()

        driver.getLifeTotal(opp) shouldBe 17
        driver.findPermanent(me, "Breya, Etherium Shaper") shouldBe null
        driver.thoptersOf(me).size shouldBe 1
    }

    test("sacrifice two artifacts: you gain 5 life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val breya = driver.castBreya(me)

        driver.activate(me, breya, driver.thoptersOf(me), mode = 2)
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 25
    }

    test("can't activate with fewer than two artifacts to sacrifice") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val breya = driver.putPermanentOnBattlefield(me, "Breya, Etherium Shaper") // no trigger, no Thopters
        driver.giveColorlessMana(me, 2)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = breya,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(breya))
            )
        ).outcome shouldNotBe Outcome.Done
    }
})
