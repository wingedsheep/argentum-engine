package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.WrigglingGrub
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Wriggling Grub (J25 #12) — "When this creature dies, create two 1/1 black and green Worm creature
 * tokens."
 */
class WrigglingGrubScenarioTest : FunSpec({

    test("dying makes two 1/1 black and green Worms") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + WrigglingGrub)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val grub = d.putCreatureOnBattlefield(d.player1, "Wriggling Grub")
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(grub)).outcome shouldBe Outcome.Done
        d.bothPass() // Bolt resolves; the Grub dies; its trigger goes on the stack
        d.bothPass() // trigger resolves

        d.findPermanent(d.player1, "Wriggling Grub") shouldBe null
        val worms = d.getPermanents(d.player1).filter { d.getCardName(it) == "Worm Token" }
        worms.size shouldBe 2
        worms.forEach { worm ->
            d.state.projectedState.getPower(worm) shouldBe 1
            d.state.projectedState.getToughness(worm) shouldBe 1
            d.state.getEntity(worm)!!.get<CardComponent>()!!.colors shouldBe setOf(Color.BLACK, Color.GREEN)
        }
    }
})
