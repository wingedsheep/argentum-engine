package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ncc.cards.MasterOfCeremonies
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Master of Ceremonies (NCC #18) — "At the beginning of your upkeep, each opponent chooses money,
 * friends, or secrets. For each player who chose money, you and that player each create a Treasure
 * token. ..."
 *
 * Pins the routing a snapshot can't see: inside the per-opponent loop the context controller is
 * rebound to the opponent, so the prompt must go to the opponent, the opponent's half must land on
 * the opponent, and "you" (via `Player.ControllerOfSource`) must still reach Master's controller.
 */
class MasterOfCeremoniesScenarioTest : FunSpec({

    val cards = TestCards.all + listOf(MasterOfCeremonies, PredefinedTokens.Treasure)

    /** Player 2 starts; Master sits on player 1's side, so the next upkeep is player 1's. */
    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(d.player1, "Master of Ceremonies")
        return d
    }

    fun GameTestDriver.passUntilDecision(maxPasses: Int = 60) {
        repeat(maxPasses) {
            if (state.pendingDecision != null) return
            state.priorityPlayerId?.let { passPriority(it) }
        }
        error("no decision was raised within $maxPasses passes (step ${state.step})")
    }

    fun GameTestDriver.count(player: EntityId, namePart: String): Int =
        getPermanents(player).count { getCardName(it)?.contains(namePart) == true }

    fun GameTestDriver.chooseAtUpkeep(optionPrefix: String) {
        passUntilDecision()
        state.step shouldBe Step.UPKEEP
        val decision = state.pendingDecision as ChooseOptionDecision
        withClue("each opponent chooses — the prompt goes to the opponent, not Master's controller") {
            decision.playerId shouldBe player2
        }
        decision.options.size shouldBe 3
        val index = decision.options.indexOfFirst { it.startsWith(optionPrefix) }
        submitDecision(player2, OptionChosenResponse(decision.id, index)).error shouldBe null
        state.pendingDecision shouldBe null
    }

    test("money — you and the opponent each create a Treasure") {
        val d = driver()
        d.chooseAtUpkeep("Money")
        d.count(d.player1, "Treasure") shouldBe 1
        d.count(d.player2, "Treasure") shouldBe 1
    }

    test("friends — you and the opponent each create a 1/1 Citizen") {
        val d = driver()
        d.chooseAtUpkeep("Friends")
        d.count(d.player1, "Citizen") shouldBe 1
        d.count(d.player2, "Citizen") shouldBe 1
    }

    test("secrets — you and the opponent each draw a card") {
        val d = driver()
        d.passUntilDecision()
        val hand1 = d.getHand(d.player1).size
        val hand2 = d.getHand(d.player2).size
        d.chooseAtUpkeep("Secrets")
        d.getHand(d.player1).size shouldBe hand1 + 1
        d.getHand(d.player2).size shouldBe hand2 + 1
        d.count(d.player1, "Treasure") shouldBe 0
    }
})
