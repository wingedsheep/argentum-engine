package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.TheAbyss
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Abyss (LEG) — "At the beginning of each player's upkeep, destroy target nonartifact creature
 * that player controls of their choice. It can't be regenerated."
 *
 * The upkeep player — not The Abyss's controller — picks the victim, and only from their own
 * nonartifact creatures.
 */
class TheAbyssScenarioTest : FunSpec({

    val cards = TestCards.all + TheAbyss

    fun GameTestDriver.passUntilDecision(maxPasses: Int = 60) {
        repeat(maxPasses) {
            if (state.pendingDecision != null) return
            state.priorityPlayerId?.let { passPriority(it) }
        }
        error("no decision was raised within $maxPasses passes (step ${state.step})")
    }

    test("the upkeep player chooses which of their nonartifact creatures is destroyed") {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(d.player1, "The Abyss")
        val mine = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.putCreatureOnBattlefield(d.player2, "Ornithopter")

        // Roll into player 2's upkeep.
        d.passUntilDecision()

        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("\"of their choice\" — the upkeep player decides") {
            decision.playerId shouldBe d.player2
        }
        withClue("only nonartifact creatures that player controls") {
            decision.legalTargets.values.flatten() shouldContainExactlyInAnyOrder listOf(bears, giant)
        }

        d.submitTargetSelection(d.player2, listOf(giant)).error shouldBe null
        repeat(4) { if (d.state.pendingDecision == null) d.bothPass() }

        d.getGraveyard(d.player2) shouldContain giant
        d.findPermanent(d.player2, "Grizzly Bears") shouldBe bears
        d.findPermanent(d.player1, "Grizzly Bears") shouldBe mine
    }
})
