package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.AccursedMarauder
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Accursed Marauder (MH3 #80) — "When this creature enters, each player sacrifices a nontoken
 * creature of their choice."
 */
class AccursedMarauderScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + AccursedMarauder)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("tokens are ignored; the controller chooses between the Marauder and another nontoken creature") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val oppToken = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.addComponent(oppToken, TokenComponent)
        val oppReal = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        val card = d.putCardInHand(d.player1, "Accursed Marauder")
        d.giveMana(d.player1, Color.BLACK, 2)
        d.castSpell(d.player1, card).outcome shouldBe Outcome.Done
        d.bothPass() // creature spell resolves, ETB trigger goes on the stack
        d.bothPass() // trigger resolves

        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe d.player1
        withClue("the Marauder itself is a nontoken creature, so it is a legal choice") {
            decision.options shouldContainExactlyInAnyOrder listOf(bears, card)
        }
        d.submitCardSelection(d.player1, listOf(bears))

        d.state.getBattlefield().contains(bears) shouldBe false
        d.state.getBattlefield().contains(card) shouldBe true
        withClue("player2's only nontoken creature is sacrificed; the token survives") {
            d.state.getBattlefield().contains(oppReal) shouldBe false
            d.state.getBattlefield().contains(oppToken) shouldBe true
        }
    }

    test("a player with only tokens sacrifices nothing") {
        val d = driver()
        val oppToken = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.addComponent(oppToken, TokenComponent)

        val card = d.putCardInHand(d.player1, "Accursed Marauder")
        d.giveMana(d.player1, Color.BLACK, 2)
        d.castSpell(d.player1, card).outcome shouldBe Outcome.Done
        d.bothPass()
        d.bothPass()

        withClue("the Marauder is player1's only nontoken creature, so it goes with no prompt") {
            d.pendingDecision shouldBe null
            d.state.getBattlefield().contains(card) shouldBe false
        }
        d.state.getBattlefield().contains(oppToken) shouldBe true
    }
})
