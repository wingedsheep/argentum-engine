package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.NightdrinkerMoroii
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Nightdrinker Moroii — {3}{B} 4/2 Vampire with flying, "when this creature enters, you lose 3
 * life", and Disguise {B}{B}.
 *
 * The card is a choice between two lines: pay {3}{B} for a 4/2 flier and eat the 3 life, or pay
 * {3} + {B}{B} across two turns and skip it entirely, because CR 702.168d makes turning face up
 * something other than entering the battlefield.
 */
class NightdrinkerMoroiiScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(NightdrinkerMoroii))
        return driver
    }

    test("hard-cast: a 4/2 flier whose controller loses 3 life on entry") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(player, "Nightdrinker Moroii")
        driver.giveMana(player, Color.BLACK, 4)

        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()
        repeat(2) { if (driver.state.priorityPlayerId != null && !driver.isPaused) driver.bothPass() }

        val moroii = driver.findPermanent(player, "Nightdrinker Moroii")
        moroii.shouldNotBeNull()
        driver.state.projectedState.getPower(moroii) shouldBe 4
        driver.state.projectedState.getToughness(moroii) shouldBe 2
        driver.state.projectedState.hasKeyword(moroii, Keyword.FLYING) shouldBe true
        driver.assertLifeTotal(player, 17)
    }

    test("the disguise line pays {3} up front and never loses the life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(player, "Nightdrinker Moroii")
        driver.giveMana(player, Color.BLACK, 3)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.bothPass()

        val moroii = driver.getPermanents(player).single {
            driver.state.getEntity(it)?.has<FaceDownComponent>() == true
        }
        driver.assertLifeTotal(player, 20)

        // Flip it for its disguise cost — still no life lost, and now it's the real 4/2 flier.
        driver.removeSummoningSickness(moroii)
        driver.giveMana(player, Color.BLACK, 2)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = moroii,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null

        driver.assertLifeTotal(player, 20)
        driver.state.projectedState.getPower(moroii) shouldBe 4
        driver.state.projectedState.hasKeyword(moroii, Keyword.FLYING) shouldBe true
    }
})
