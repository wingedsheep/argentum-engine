package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.isd.cards.SkirsdagHighPriest
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Skirsdag High Priest (ISD) — {1}{B} Creature — Human Cleric 1/2
 *
 * "Morbid — {T}, Tap two untapped creatures you control: Create a 5/5 black Demon creature token
 *  with flying. Activate only if a creature died this turn."
 *
 * Two things to prove: the morbid gate really blocks activation until something has died this turn,
 * and the composite cost ({T} on the source plus two *other* tapped creatures) pays out the Demon.
 */
class SkirsdagHighPriestScenarioTest : FunSpec({

    val demonAbilityId = SkirsdagHighPriest.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SkirsdagHighPriest)
        return driver
    }

    test("morbid gate: can't activate before a creature has died this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val priest = driver.putCreatureOnBattlefield(me, "Skirsdag High Priest")
        driver.removeSummoningSickness(priest)
        val helperA = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val helperB = driver.putCreatureOnBattlefield(me, "Savannah Lions")

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = me,
                sourceId = priest,
                abilityId = demonAbilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(helperA, helperB))
            )
        )

        driver.findPermanent(me, "Demon Token") shouldBe null
    }

    test("after a creature dies: tapping the Priest and two other creatures makes a 5/5 flying Demon") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val priest = driver.putCreatureOnBattlefield(me, "Skirsdag High Priest")
        driver.removeSummoningSickness(priest)
        val helperA = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val helperB = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val fodder = driver.putCreatureOnBattlefield(me, "Goblin Guide")

        // Something dies this turn — morbid is now satisfied.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(fodder)).isSuccess shouldBe true
        driver.bothPass()
        driver.getGraveyardCardNames(me).contains("Goblin Guide") shouldBe true

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = priest,
                abilityId = demonAbilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(helperA, helperB))
            )
        )

        // The whole composite cost was paid: the Priest itself plus both helpers are tapped.
        driver.isTapped(priest) shouldBe true
        driver.isTapped(helperA) shouldBe true
        driver.isTapped(helperB) shouldBe true

        driver.bothPass() // the ability resolves

        val demon = driver.findPermanent(me, "Demon Token")
        demon shouldNotBe null
        val projected = driver.state.projectedState
        projected.getPower(demon!!) shouldBe 5
        projected.getToughness(demon) shouldBe 5
        projected.hasKeyword(demon, Keyword.FLYING) shouldBe true
    }
})
