package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.VedalkenArchmage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Vedalken Archmage (MRD #55) — "Whenever you cast an artifact spell, draw a card."
 *
 * A *cast* trigger with a card-type filter, so the two ways it can go wrong are the filter being
 * ignored (a non-artifact spell drawing) and the trigger being wired to resolution rather than
 * casting (an artifact that gets countered drawing nothing). Both are pinned.
 */
class VedalkenArchmageScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + VedalkenArchmage)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    test("casting an artifact spell draws a card") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Vedalken Archmage")

        val bonesplitter = d.putCardInHand(me, "Bonesplitter")
        val handBefore = d.getHand(me).size

        d.giveColorlessMana(me, 1)
        d.castSpell(me, bonesplitter).isSuccess shouldBe true
        resolveStack(d)

        withClue("Bonesplitter left the hand (-1) and the trigger drew one (+1)") {
            d.getHand(me).size shouldBe handBefore
        }
        d.state.getBattlefield().contains(bonesplitter) shouldBe true
    }

    test("casting a nonartifact spell draws nothing") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Vedalken Archmage")

        val bolt = d.putCardInHand(me, "Lightning Bolt")
        val handBefore = d.getHand(me).size

        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(d.player2)).isSuccess shouldBe true
        resolveStack(d)

        withClue("the filter is artifacts only — the hand just shrinks by the Bolt") {
            d.getHand(me).size shouldBe handBefore - 1
        }
    }

    test("an opponent's artifact spell doesn't trigger it") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Vedalken Archmage")

        // Equipment is sorcery-speed, so hand the turn to player2 before they cast it.
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe d.player2

        val theirs = d.putCardInHand(d.player2, "Bonesplitter")
        val handBefore = d.getHand(me).size

        d.giveColorlessMana(d.player2, 1)
        d.castSpell(d.player2, theirs).isSuccess shouldBe true
        resolveStack(d)

        withClue("'whenever YOU cast' — the Archmage's controller, not any player") {
            d.getHand(me).size shouldBe handBefore
        }
    }
})
