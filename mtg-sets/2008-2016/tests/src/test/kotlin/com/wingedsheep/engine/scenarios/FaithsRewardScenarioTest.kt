package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.FaithsReward
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Faith's Reward — {3}{W} Instant (Magic 2013 #14)
 *
 * "Return to the battlefield all permanent cards in your graveyard that were put there from the
 *  battlefield this turn."
 *
 * Pairs a creature that died this turn with a permanent card that was merely put into the
 * graveyard, and an opponent's creature that died this turn, checking only the first comes back.
 */
class FaithsRewardScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + FaithsReward)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.bolt(target: EntityId) {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, bolt, targets = listOf(target)).error shouldBe null
        bothPass()
    }

    test("returns only your permanent cards that went to your graveyard from the battlefield this turn") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        val neverOnBattlefield = d.putCardInGraveyard(d.player1, "Savannah Lions")

        d.bolt(courser)
        d.bolt(theirs)
        withClue("both creatures died") {
            (courser in d.getGraveyard(d.player1)) shouldBe true
            (theirs in d.getGraveyard(d.player2)) shouldBe true
        }

        val reward = d.putCardInHand(d.player1, "Faith's Reward")
        d.giveMana(d.player1, Color.WHITE, 4)
        d.castSpell(d.player1, reward).error shouldBe null
        d.bothPass()

        withClue("the creature that died this turn is back on the battlefield") {
            d.findPermanent(d.player1, "Centaur Courser") shouldNotBe null
            (courser in d.getGraveyard(d.player1)) shouldBe false
        }
        withClue("a permanent card that never left the battlefield for your graveyard stays put") {
            (neverOnBattlefield in d.getGraveyard(d.player1)) shouldBe true
        }
        withClue("an opponent's creature that died this turn stays in their graveyard") {
            (theirs in d.getGraveyard(d.player2)) shouldBe true
            d.findPermanent(d.player1, "Savannah Lions") shouldBe null
        }
    }
})
