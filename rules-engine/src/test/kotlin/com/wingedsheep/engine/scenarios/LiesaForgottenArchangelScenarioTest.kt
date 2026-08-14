package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.LiesaForgottenArchangel
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LiesaForgottenArchangelScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + LiesaForgottenArchangel)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            skipMulligans = true,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun destroy(driver: GameTestDriver, caster: EntityId, victim: EntityId) {
        val murder = driver.putCardInHand(caster, "Murder")
        driver.giveMana(caster, Color.BLACK, 3)
        driver.castSpell(caster, murder, listOf(victim)).isSuccess shouldBe true
        driver.bothPass()
    }

    test("another nontoken creature you control returns to its owner's hand at the next end step") {
        val driver = newDriver()
        val player = driver.player1

        driver.putCreatureOnBattlefield(player, "Liesa, Forgotten Archangel")
        val creature = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        destroy(driver, player, creature)
        driver.bothPass() // resolve Liesa's dies trigger and schedule the delayed return

        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(creature) shouldBe true
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.state.getZone(ZoneKey(player, Zone.HAND)).contains(creature) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(creature) shouldBe false
    }

    test("Liesa does not return herself") {
        val driver = newDriver()
        val player = driver.player1

        val liesa = driver.putCreatureOnBattlefield(player, "Liesa, Forgotten Archangel")
        destroy(driver, player, liesa)
        driver.passPriorityUntil(Step.END)

        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(liesa) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.HAND)).contains(liesa) shouldBe false
    }

    test("an opponent's creature is exiled instead of dying") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(player, "Liesa, Forgotten Archangel")
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        destroy(driver, player, creature)

        driver.state.getZone(ZoneKey(opponent, Zone.EXILE)).contains(creature) shouldBe true
        driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)).contains(creature) shouldBe false
    }
})
