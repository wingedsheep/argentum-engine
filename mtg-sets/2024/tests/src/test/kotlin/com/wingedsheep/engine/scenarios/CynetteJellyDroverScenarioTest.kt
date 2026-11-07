package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.CynetteJellyDrover
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cynette, Jelly Drover (J25 #35) — "When Cynette enters or dies, create a 1/1 blue Jellyfish
 * creature token with flying. Creatures you control with flying get +1/+1."
 */
class CynetteJellyDroverScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + CynetteJellyDrover)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.jellyfish() = getPermanents(player1).filter { getCardName(it) == "Jellyfish Token" }

    test("entering makes a flying Jellyfish that Cynette pumps; dying makes another") {
        val d = driver()
        val cynetteCard = d.putCardInHand(d.player1, "Cynette, Jelly Drover")
        d.giveMana(d.player1, Color.BLUE, 4)
        d.castSpell(d.player1, cynetteCard).outcome shouldBe Outcome.Done
        d.bothPass() // Cynette resolves; enters trigger goes on the stack
        d.bothPass() // trigger resolves

        val cynette = d.findPermanent(d.player1, "Cynette, Jelly Drover")!!
        val first = d.jellyfish().single()
        val projected = d.state.projectedState
        withClue("the flying Jellyfish gets +1/+1; Cynette herself has no flying") {
            projected.hasKeyword(first, Keyword.FLYING) shouldBe true
            projected.getPower(first) shouldBe 2
            projected.getToughness(first) shouldBe 2
            projected.getPower(cynette) shouldBe 2
        }

        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(cynette)).outcome shouldBe Outcome.Done
        d.bothPass() // Bolt resolves; Cynette dies; dies trigger goes on the stack
        d.bothPass() // trigger resolves

        d.findPermanent(d.player1, "Cynette, Jelly Drover") shouldBe null
        val jellies = d.jellyfish()
        withClue("the dies trigger made a second Jellyfish; with Cynette gone both are 1/1") {
            jellies.size shouldBe 2
            jellies.forEach { d.state.projectedState.getPower(it) shouldBe 1 }
        }
    }
})
