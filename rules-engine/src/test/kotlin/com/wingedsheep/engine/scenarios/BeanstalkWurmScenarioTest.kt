package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.BeanstalkWurm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Beanstalk Wurm // Plant Beans (WOE) — Adventure (CR 715).
 *
 * Creature face: {4}{G} 5/4 Plant Wurm with reach.
 * Adventure face: Plant Beans {1}{G}, Sorcery — Adventure, "You may play an additional land this turn."
 */
class BeanstalkWurmScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BeanstalkWurm)
        return driver
    }

    fun startAtMain(driver: GameTestDriver): EntityId {
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40, "Grizzly Bears" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return player
    }

    test("casting Plant Beans grants an extra land drop and exiles the card for later") {
        val driver = createDriver()
        val player = startAtMain(driver)

        val wurm = driver.putCardInHand(player, "Beanstalk Wurm")
        driver.giveMana(player, Color.GREEN, 2) // {1}{G}

        // Cast the Adventure face (faceIndex = 0), not the creature.
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = wurm,
                faceIndex = 0,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.isPaused shouldBe false

        // Land drops went from 1 to 2 for this turn.
        driver.state.getEntity(player)?.get<LandDropsComponent>()?.remaining shouldBe 2

        // The card exiled itself on resolution (CR 715.3d) and granted a may-play permission.
        driver.getExile(player) shouldContain wurm
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldNotContain wurm
        driver.state.mayPlayPermissions.any { wurm in it.cardIds && it.controllerId == player } shouldBe true

        // Two lands can actually be played this turn.
        val forest1 = driver.putCardInHand(player, "Forest")
        val forest2 = driver.putCardInHand(player, "Forest")
        val forest3 = driver.putCardInHand(player, "Forest")
        driver.playLand(player, forest1).isSuccess shouldBe true
        driver.playLand(player, forest2).isSuccess shouldBe true
        // ...but not a third.
        driver.submit(PlayLand(player, forest3)).isSuccess shouldBe false
    }

    test("the creature can be cast from exile afterwards as a 5/4 with reach") {
        val driver = createDriver()
        val player = startAtMain(driver)

        val wurm = driver.putCardInHand(player, "Beanstalk Wurm")
        driver.giveMana(player, Color.GREEN, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = wurm,
                faceIndex = 0,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.getExile(player) shouldContain wurm

        // Cast the creature face from exile — {4}{G}.
        driver.giveMana(player, Color.GREEN, 5)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = wurm,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.getPermanents(player) shouldContain wurm
        driver.getExile(player) shouldNotContain wurm

        val projected = driver.state.projectedState
        projected.getPower(wurm) shouldBe 5
        projected.getToughness(wurm) shouldBe 4
        projected.hasKeyword(wurm, Keyword.REACH) shouldBe true
    }
})
