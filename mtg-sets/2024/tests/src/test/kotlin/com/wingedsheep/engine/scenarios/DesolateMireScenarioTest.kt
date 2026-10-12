package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.DesolateMire
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DesolateMireScenarioTest : FunSpec({
    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DesolateMire)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("paying one mana produces white and black immediately") {
        val driver = setup()
        val player = driver.activePlayer!!
        val mire = driver.putPermanentOnBattlefield(player, "Desolate Mire")
        driver.giveColorlessMana(player, 1)
        driver.submit(ActivateAbility(player, mire, DesolateMire.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        val pool = driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!
        pool.colorless shouldBe 0
        pool.white shouldBe 1
        pool.black shouldBe 1
    }

    test("cannot use its output to pay its own activation") {
        val driver = setup()
        val player = driver.activePlayer!!
        val mire = driver.putPermanentOnBattlefield(player, "Desolate Mire")
        driver.submit(ActivateAbility(player, mire, DesolateMire.activatedAbilities.single().id)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        val pool = driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!
        pool.white shouldBe 0
        pool.black shouldBe 0
    }
})
