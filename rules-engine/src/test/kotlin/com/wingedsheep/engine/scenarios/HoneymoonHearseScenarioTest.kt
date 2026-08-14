package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vow.cards.HoneymoonHearse
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Honeymoon Hearse (VOW) — {2}{R} Artifact — Vehicle 5/5
 *
 * "Trample
 *  Tap two untapped creatures you control: This Vehicle becomes an artifact creature until end of turn."
 *
 * A crewless Vehicle: it isn't a creature until its own ability animates it, and the animation lasts
 * only for the turn. Printed trample rides along once it is one.
 */
class HoneymoonHearseScenarioTest : FunSpec({

    val animateAbilityId = HoneymoonHearse.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HoneymoonHearse)
        return driver
    }

    test("tapping two creatures animates the Vehicle into a 5/5 trampler for the turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val hearse = driver.putPermanentOnBattlefield(me, "Honeymoon Hearse")
        val crewA = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val crewB = driver.putCreatureOnBattlefield(me, "Savannah Lions")

        // A Vehicle is not a creature on its own.
        driver.state.projectedState.isCreature(hearse) shouldBe false

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = hearse,
                abilityId = animateAbilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(crewA, crewB))
            )
        )
        driver.isTapped(crewA) shouldBe true
        driver.isTapped(crewB) shouldBe true
        // The Vehicle itself is not tapped — there is no {T} in the cost.
        driver.isTapped(hearse) shouldBe false

        driver.bothPass() // the ability resolves

        val projected = driver.state.projectedState
        projected.isCreature(hearse) shouldBe true
        projected.getPower(hearse) shouldBe 5
        projected.getToughness(hearse) shouldBe 5
        projected.hasKeyword(hearse, Keyword.TRAMPLE) shouldBe true
    }
})
