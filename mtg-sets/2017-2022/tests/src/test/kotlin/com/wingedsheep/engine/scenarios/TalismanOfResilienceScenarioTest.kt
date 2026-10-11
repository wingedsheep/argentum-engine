package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh1.cards.TalismanOfResilience
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Talisman of Resilience: {T}: Add {C}. / {T}: Add {B} or {G}. This artifact deals 1 damage to you.
 * The colourless line is free; each coloured line costs 1 life.
 */
class TalismanOfResilienceScenarioTest : FunSpec({

    val colorless = TalismanOfResilience.activatedAbilities[0].id
    val black = TalismanOfResilience.activatedAbilities[1].id
    val green = TalismanOfResilience.activatedAbilities[2].id

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TalismanOfResilience)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("{T}: Add {C} costs no life") {
        val driver = setup()
        val player = driver.activePlayer!!
        val talisman = driver.putPermanentOnBattlefield(player, "Talisman of Resilience")

        driver.submit(ActivateAbility(player, talisman, colorless)).outcome shouldBe Outcome.Done
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
        driver.getLifeTotal(player) shouldBe 20
    }

    test("{T}: Add {B} deals 1 damage to you") {
        val driver = setup()
        val player = driver.activePlayer!!
        val talisman = driver.putPermanentOnBattlefield(player, "Talisman of Resilience")

        driver.submit(ActivateAbility(player, talisman, black)).outcome shouldBe Outcome.Done
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.black shouldBe 1
        driver.getLifeTotal(player) shouldBe 19
    }

    test("{T}: Add {G} deals 1 damage to you") {
        val driver = setup()
        val player = driver.activePlayer!!
        val talisman = driver.putPermanentOnBattlefield(player, "Talisman of Resilience")

        driver.submit(ActivateAbility(player, talisman, green)).outcome shouldBe Outcome.Done
        driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!.green shouldBe 1
        driver.getLifeTotal(player) shouldBe 19
    }
})
