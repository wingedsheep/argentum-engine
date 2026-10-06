package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Fastbond
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fastbond (LEA #192) — "You may play any number of lands on each of your turns. Whenever you
 * play a land, if it wasn't the first land you played this turn, this enchantment deals 1 damage
 * to you."
 */
class FastbondScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Fastbond)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    test("plays four lands in a turn and takes 1 damage for each after the first") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Fastbond")

        repeat(4) {
            d.playLand(me, d.putCardInHand(me, "Forest")).outcome shouldBe Outcome.Done
            resolveStack(d)
        }

        withClue("three non-first lands, 1 damage each; the opponent is untouched") {
            d.getLifeTotal(me) shouldBe 17
            d.getLifeTotal(d.getOpponent(me)) shouldBe 20
        }
    }

    test("a land played before Fastbond entered still makes the next one non-first") {
        val d = driver()
        val me = d.activePlayer!!
        d.playLand(me, d.putCardInHand(me, "Forest")).outcome shouldBe Outcome.Done
        d.putPermanentOnBattlefield(me, "Fastbond")

        d.playLand(me, d.putCardInHand(me, "Forest")).outcome shouldBe Outcome.Done
        resolveStack(d)

        d.getLifeTotal(me) shouldBe 19
    }

    test("an effect-granted extra land drop doesn't hide the second land from the tracker") {
        // Explore-style extra drops raise only `remaining`; the lands-played count must still
        // see the second land as non-first.
        val d = driver()
        val me = d.activePlayer!!
        d.replaceState(d.state.updateEntity(me) { it.with(LandDropsComponent(remaining = 2, maxPerTurn = 1)) })
        d.putPermanentOnBattlefield(me, "Fastbond")

        repeat(2) {
            d.playLand(me, d.putCardInHand(me, "Forest")).outcome shouldBe Outcome.Done
            resolveStack(d)
        }

        d.getLifeTotal(me) shouldBe 19
    }
})
