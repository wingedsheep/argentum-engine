package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Piracy (P02 #42).
 *
 * {U}{U} Sorcery
 * "Until end of turn, you may tap lands you don't control for mana. Spend this mana only to cast
 * spells."
 */
class PiracyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castPiracy(driver: GameTestDriver) {
        val me = driver.player1
        driver.putLandOnBattlefield(me, "Island")
        driver.putLandOnBattlefield(me, "Island")
        driver.castSpell(me, driver.putCardInHand(me, "Piracy")).error shouldBe null
        driver.bothPass()
        driver.getGraveyardCardNames(me) shouldContain "Piracy"
    }

    test("the caster taps an opponent's land and gets spells-only mana") {
        val driver = createDriver()
        val me = driver.player1
        val forest = driver.putLandOnBattlefield(driver.player2, "Forest")
        castPiracy(driver)

        val tap = driver.legalActions(me)
            .single { it.isManaAbility && (it.action as? ActivateAbility)?.sourceId == forest }
        driver.submit(tap.action).error shouldBe null

        driver.isTapped(forest) shouldBe true
        val pool = driver.state.getEntity(me)!!.get<ManaPoolComponent>()!!
        pool.restrictedMana.single().color shouldBe Color.GREEN
        pool.restrictedMana.single().restriction shouldBe ManaRestriction.SpellsOnly
    }

    test("auto-pay casts a spell off the opponent's lands") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2
        driver.putLandOnBattlefield(opponent, "Forest")
        driver.putLandOnBattlefield(opponent, "Forest")
        castPiracy(driver)

        driver.castSpell(me, driver.putCardInHand(me, "Grizzly Bears")).error shouldBe null
        driver.bothPass()

        driver.getLands(opponent).forEach { driver.isTapped(it) shouldBe true }
        driver.getCreatures(me).map { driver.getCardName(it) } shouldContain "Grizzly Bears"
    }

    test("without Piracy the opponent's lands are not offered") {
        val driver = createDriver()
        val forest = driver.putLandOnBattlefield(driver.player2, "Forest")

        driver.legalActions(driver.player1)
            .filter { (it.action as? ActivateAbility)?.sourceId == forest }
            .shouldBeEmpty()
    }
})
