package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frc.cards.AvacynAngelOfHorror
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Avacyn, Angel of Horror (FRC #7): "Whenever Avacyn or another nontoken creature you control
 * dies, return that card to the battlefield under your control at the beginning of the next end
 * step."
 *
 * The self half and the "another" half are separate triggers; both must schedule a delayed return
 * that waits for the end step rather than returning the card immediately.
 */
class AvacynAngelOfHorrorScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AvacynAngelOfHorror)
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
        driver.castSpell(caster, murder, listOf(victim)).outcome shouldBe Outcome.Done
        driver.bothPass()
    }

    test("another nontoken creature you control returns to the battlefield at the next end step") {
        val driver = newDriver()
        val player = driver.player1

        driver.putCreatureOnBattlefield(player, "Avacyn, Angel of Horror")
        val creature = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        destroy(driver, player, creature)
        driver.bothPass() // resolve the dies trigger, scheduling the delayed return

        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(creature) shouldBe true
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).contains(creature) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(creature) shouldBe false
    }

    test("Avacyn returns herself at the next end step") {
        val driver = newDriver()
        val player = driver.player1

        val avacyn = driver.putCreatureOnBattlefield(player, "Avacyn, Angel of Horror")
        destroy(driver, player, avacyn)
        driver.bothPass() // resolve the dies trigger, scheduling the delayed return

        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(avacyn) shouldBe true
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).contains(avacyn) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(avacyn) shouldBe false
    }
})
