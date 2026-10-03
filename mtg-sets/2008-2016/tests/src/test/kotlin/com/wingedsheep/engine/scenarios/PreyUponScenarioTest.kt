package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.isd.cards.PreyUpon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Prey Upon (ISD #200 — {G} Sorcery)
 * "Target creature you control fights target creature you don't control."
 */
class PreyUponScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PreyUpon)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.damageOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("your creature and theirs deal damage equal to their power to each other") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions") // 1/1
        val spell = driver.putCardInHand(me, "Prey Upon")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, spell, listOf(courser, lions))
        driver.bothPass()

        driver.findPermanent(opponent, "Savannah Lions") shouldBe null
        driver.getGraveyardCardNames(opponent).contains("Savannah Lions") shouldBe true
        driver.damageOn(courser) shouldBe 1
    }

    test("a bigger opposing creature kills yours") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions") // 1/1
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature") // 5/5
        val spell = driver.putCardInHand(me, "Prey Upon")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, spell, listOf(lions, force))
        driver.bothPass()

        driver.findPermanent(me, "Savannah Lions") shouldBe null
        driver.damageOn(force) shouldBe 1
    }
})
