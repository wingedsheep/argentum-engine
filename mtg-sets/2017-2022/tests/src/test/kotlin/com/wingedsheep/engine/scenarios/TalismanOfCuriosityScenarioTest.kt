package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh1.cards.TalismanOfCuriosity
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Talisman of Curiosity (MH1 #232): {T}: Add {C}. / {T}: Add {G} or {U}. This artifact deals 1
 * damage to you. The colorless line causes no damage; each colored line deals 1 damage.
 */
class TalismanOfCuriosityScenarioTest : FunSpec({

    val colorless = TalismanOfCuriosity.activatedAbilities[0].id
    val green = TalismanOfCuriosity.activatedAbilities[1].id
    val blue = TalismanOfCuriosity.activatedAbilities[2].id

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TalismanOfCuriosity)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("{T}: Add {C} costs no life") {
        val driver = setup()
        val player = driver.activePlayer!!
        val talisman = driver.putPermanentOnBattlefield(player, "Talisman of Curiosity")

        driver.submit(ActivateAbility(player, talisman, colorless)).outcome shouldBe Outcome.Done
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
        driver.getLifeTotal(player) shouldBe 20
    }

    test("{T}: Add {G} deals 1 damage to you") {
        val driver = setup()
        val player = driver.activePlayer!!
        val talisman = driver.putPermanentOnBattlefield(player, "Talisman of Curiosity")

        driver.submit(ActivateAbility(player, talisman, green)).outcome shouldBe Outcome.Done
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.green shouldBe 1
        driver.getLifeTotal(player) shouldBe 19
    }

    test("{T}: Add {U} deals 1 damage to you") {
        val driver = setup()
        val player = driver.activePlayer!!
        val talisman = driver.putPermanentOnBattlefield(player, "Talisman of Curiosity")

        driver.submit(ActivateAbility(player, talisman, blue)).outcome shouldBe Outcome.Done
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.blue shouldBe 1
        driver.getLifeTotal(player) shouldBe 19
    }
})
