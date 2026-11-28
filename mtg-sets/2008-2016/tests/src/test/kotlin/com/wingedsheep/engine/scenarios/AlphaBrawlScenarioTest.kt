package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.cards.AlphaBrawl
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Alpha Brawl (DKA #82) — "Target creature an opponent controls deals damage equal to its power to
 * each other creature that player controls, then each of those creatures deals damage equal to its
 * power to that creature."
 *
 * Pins: the target's damage is undivided and hits only its controller's other creatures; creatures
 * dealt lethal damage by the target still deal their damage back (no state-based actions between
 * the halves, 2011-01-22 ruling); and the return damage is summed onto the target.
 */
class AlphaBrawlScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + AlphaBrawl)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.brawl(target: EntityId) {
        val spell = putCardInHand(player1, "Alpha Brawl")
        giveMana(player1, Color.RED, 8)
        castSpell(player1, spell, listOf(target)).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("creatures dealt lethal damage by the target still hit it back") {
        val d = driver()
        val alpha = d.putCreatureOnBattlefield(d.player2, "Force of Nature") // 5/5
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears") // 2/2
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser") // 3/3
        val mine = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.brawl(alpha)

        val theirs = d.getCreatures(d.player2)
        withClue("the 5/5 deals 5 undivided damage to each other creature its controller has") {
            theirs.contains(bears) shouldBe false
            theirs.contains(courser) shouldBe false
        }
        withClue("2 + 3 return damage from the doomed creatures kills the 5/5") {
            theirs.contains(alpha) shouldBe false
        }
        withClue("the caster's own creature is not part of the brawl") {
            d.getCreatures(d.player1).contains(mine) shouldBe true
        }
    }

    test("a survivor hits back and the target can die to the return damage alone") {
        val d = driver()
        val alpha = d.putCreatureOnBattlefield(d.player2, "Centaur Courser") // 3/3
        val big = d.putCreatureOnBattlefield(d.player2, "Force of Nature") // 5/5

        d.brawl(alpha)

        val theirs = d.getCreatures(d.player2)
        withClue("3 damage doesn't kill the 5/5") { theirs.contains(big) shouldBe true }
        withClue("the 5/5 deals 5 back to the 3/3") { theirs.contains(alpha) shouldBe false }
    }
})
