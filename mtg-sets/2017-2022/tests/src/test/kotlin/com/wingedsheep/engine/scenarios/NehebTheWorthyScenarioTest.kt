package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.AhnCropCrasher
import com.wingedsheep.mtg.sets.definitions.akh.cards.NehebTheWorthy
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Neheb, the Worthy (AKH #203) — "Other Minotaurs you control have first strike. As long as you
 * have one or fewer cards in hand, Minotaurs you control get +2/+0."
 *
 * The anthem hangs on a live hand-size condition and includes Neheb itself, while the first-strike
 * grant skips Neheb and both ignore non-Minotaurs. The tests move the hand across the 1/2 boundary
 * and read projected power each time.
 */
class NehebTheWorthyScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + NehebTheWorthy + AhnCropCrasher)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Leave exactly [size] of the player's current hand cards in hand. */
    fun GameTestDriver.trimHand(playerId: EntityId, size: Int) {
        val hand = ZoneKey(playerId, Zone.HAND)
        val kept = (state.zones[hand] ?: emptyList()).take(size)
        replaceState(state.copy(zones = state.zones + (hand to kept)))
    }

    test("with a full hand Minotaurs get no bonus, but other Minotaurs still have first strike") {
        val d = driver()
        val neheb = d.putCreatureOnBattlefield(d.player1, "Neheb, the Worthy")
        val crasher = d.putCreatureOnBattlefield(d.player1, "Ahn-Crop Crasher")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        val projected = d.state.projectedState
        projected.getPower(neheb) shouldBe 2
        projected.getPower(crasher) shouldBe 3
        projected.getPower(lions) shouldBe 1
        projected.hasKeyword(crasher, Keyword.FIRST_STRIKE) shouldBe true
        projected.hasKeyword(neheb, Keyword.FIRST_STRIKE) shouldBe true
        projected.hasKeyword(lions, Keyword.FIRST_STRIKE) shouldBe false
    }

    test("one card in hand turns the anthem on for every Minotaur, Neheb included") {
        val d = driver()
        val neheb = d.putCreatureOnBattlefield(d.player1, "Neheb, the Worthy")
        val crasher = d.putCreatureOnBattlefield(d.player1, "Ahn-Crop Crasher")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val opposingCrasher = d.putCreatureOnBattlefield(d.player2, "Ahn-Crop Crasher")

        d.trimHand(d.player1, 2)
        withClue("two cards in hand is one too many") {
            d.state.projectedState.getPower(neheb) shouldBe 2
        }

        d.trimHand(d.player1, 1)
        val projected = d.state.projectedState
        projected.getPower(neheb) shouldBe 4
        projected.getToughness(neheb) shouldBe 2
        projected.getPower(crasher) shouldBe 5
        withClue("non-Minotaurs and opposing Minotaurs are untouched") {
            projected.getPower(lions) shouldBe 1
            projected.getPower(opposingCrasher) shouldBe 3
            projected.hasKeyword(opposingCrasher, Keyword.FIRST_STRIKE) shouldBe false
        }
    }

    test("an empty hand keeps the bonus, and an opponent's hand size does not matter") {
        val d = driver()
        val neheb = d.putCreatureOnBattlefield(d.player1, "Neheb, the Worthy")

        d.trimHand(d.player2, 0)
        d.state.projectedState.getPower(neheb) shouldBe 2

        d.trimHand(d.player1, 0)
        d.state.projectedState.getPower(neheb) shouldBe 4
    }
})
