package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaMountain292
import com.wingedsheep.mtg.sets.definitions.m14.cards.BurningEarth
import com.wingedsheep.mtg.sets.definitions.m14.cards.EncroachingWastes
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Burning Earth (M14) — {3}{R} Enchantment.
 *
 * "Whenever a player taps a nonbasic land for mana, this enchantment deals 1 damage to that player."
 *
 * First card on `tapsLandForMana` with a land filter: pins that a nonbasic land triggers it (damaging
 * whoever tapped it, via the stack) and that a basic land does not.
 */
class BurningEarthScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all.filter { it.name != "Mountain" })
        driver.registerCard(AlphaMountain292)
        driver.registerCard(BurningEarth)
        driver.registerCard(EncroachingWastes)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val wastesManaAbility = EncroachingWastes.activatedAbilities.first().id
    val mountainAbility = AlphaMountain292.activatedAbilities.single().id

    test("an opponent tapping a nonbasic land for mana is dealt 1 damage, via the stack") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Burning Earth")
        val theirWastes = driver.putPermanentOnBattlefield(opponent, "Encroaching Wastes")

        driver.passPriority(you)
        driver.submit(ActivateAbility(opponent, theirWastes, wastesManaAbility)).error shouldBe null

        driver.stackSize shouldBe 1
        driver.getLifeTotal(opponent) shouldBe 20
        driver.bothPass()
        driver.getLifeTotal(opponent) shouldBe 19
        driver.getLifeTotal(you) shouldBe 20
    }

    test("tapping a basic land for mana doesn't trigger it") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Burning Earth")
        val mountain = driver.putPermanentOnBattlefield(you, "Mountain")

        driver.submit(ActivateAbility(you, mountain, mountainAbility)).error shouldBe null

        driver.stackSize shouldBe 0
        driver.getLifeTotal(you) shouldBe 20
    }
})
