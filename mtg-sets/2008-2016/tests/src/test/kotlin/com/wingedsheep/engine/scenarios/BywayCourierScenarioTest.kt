package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.BywayCourier
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Byway Courier (Shadows over Innistrad #196) — {2}{G} Creature — Human Scout 3/2.
 *
 * "When this creature dies, investigate."
 */
class BywayCourierScenarioTest : FunSpec({

    test("when it dies, its controller investigates") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BywayCourier)
        driver.registerCard(PredefinedTokens.Clue)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val courier = driver.putCreatureOnBattlefield(me, "Byway Courier")
        fun clues() = driver.getPermanents(me).count {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Clue"
        }
        clues() shouldBe 0

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(courier)).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.getGraveyard(me) shouldContain courier
        clues() shouldBe 1
    }
})
