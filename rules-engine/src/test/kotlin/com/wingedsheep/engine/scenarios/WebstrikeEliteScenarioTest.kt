package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Webstrike Elite — Reach, Cycling {X}{G}{G}, and "When you cycle this card, destroy up to one
 * target artifact or enchantment with mana value X."
 *
 * Cycling is an activated ability (CR 702.29a), so X is announced as the ability is activated
 * (CR 107.3a). These cases pin that the announced X reaches the trigger: it gates which permanents
 * are legal targets (`manaValueEqualsX()` is *equals*, not "or less"), X = 0 is a legal
 * announcement that simply finds nothing, and — per the card's rulings — cycling is legal with no
 * matching permanent at all because the two abilities are independent.
 */
class WebstrikeEliteScenarioTest : ScenarioTestBase() {

    init {
        test("cycling for X=3 destroys a mana-value-3 artifact") {
            // Bottle Gnomes is a {3} artifact, so cycling for X=3 must find it.
            val game = eliteGame(opposing = listOf("Bottle Gnomes"), forests = 5)
            val gnomes = game.findPermanent("Bottle Gnomes")!!

            val cycle = game.cycleCard(1, "Webstrike Elite", xValue = 3)
            withClue("Cycling for X=3 should succeed: ${cycle.error}") { cycle.error shouldBe null }
            if (game.hasPendingDecision()) game.selectTargets(listOf(gnomes))
            game.resolveStack()

            withClue("The mana-value-3 artifact was destroyed") {
                game.findPermanent("Bottle Gnomes") shouldBe null
                game.isInGraveyard(2, "Bottle Gnomes") shouldBe true
            }
            withClue("The Elite itself was discarded to pay the cycling cost") {
                game.isInGraveyard(1, "Webstrike Elite") shouldBe true
            }
        }

        test("the announced X gates targeting — a mana value 3 artifact is no target for X=2") {
            val game = eliteGame(opposing = listOf("Bottle Gnomes"), forests = 5)

            val cycle = game.cycleCard(1, "Webstrike Elite", xValue = 2)
            withClue("Cycling for X=2 should still succeed: ${cycle.error}") { cycle.error shouldBe null }
            if (game.hasPendingDecision()) game.skipTargets()
            game.resolveStack()

            withClue("\"mana value X\" is an exact match, so the {3} artifact survives X=2") {
                (game.findPermanent("Bottle Gnomes") != null) shouldBe true
                game.isInGraveyard(2, "Bottle Gnomes") shouldBe false
            }
        }

        test("cycling with no matching permanent still draws — the two abilities are independent") {
            val game = eliteGame(opposing = emptyList(), forests = 5)
            val handBefore = game.handSize(1)

            val cycle = game.cycleCard(1, "Webstrike Elite", xValue = 2)
            withClue("Cycling is legal with nothing to destroy: ${cycle.error}") {
                cycle.error shouldBe null
            }
            if (game.hasPendingDecision()) game.skipTargets()
            game.resolveStack()

            withClue("The cycling ability drew a card, replacing the discarded Elite") {
                game.handSize(1) shouldBe handBefore
                game.isInGraveyard(1, "Webstrike Elite") shouldBe true
            }
        }

        test("X=0 is a legal announcement and costs only the coloured part of the cycling cost") {
            // Two Forests pay {0}{G}{G} exactly — proof the {X} was charged as 0, not skipped.
            val game = eliteGame(opposing = listOf("Bottle Gnomes"), forests = 2)

            val cycle = game.cycleCard(1, "Webstrike Elite", xValue = 0)
            withClue("Cycling for X=0 should succeed on two Forests: ${cycle.error}") {
                cycle.error shouldBe null
            }
            if (game.hasPendingDecision()) game.skipTargets()
            game.resolveStack()

            withClue("Nothing has mana value 0, so the Gnomes survive") {
                game.isInGraveyard(2, "Bottle Gnomes") shouldBe false
            }
            withClue("The Elite was still cycled") {
                game.isInGraveyard(1, "Webstrike Elite") shouldBe true
            }
        }

        test("submitting no X raises a ChooseNumber decision — the client's path") {
            val game = eliteGame(opposing = listOf("Bottle Gnomes"), forests = 5)
            val gnomes = game.findPermanent("Bottle Gnomes")!!

            val cycle = game.cycleCard(1, "Webstrike Elite")
            withClue("A bare CycleCard on an {X} cost pauses instead of defaulting X to 0") {
                cycle.error shouldBe null
                game.hasPendingDecision() shouldBe true
            }

            game.chooseNumber(3)
            if (game.hasPendingDecision()) game.selectTargets(listOf(gnomes))
            game.resolveStack()

            withClue("The X answered at the decision reached the trigger") {
                game.isInGraveyard(2, "Bottle Gnomes") shouldBe true
            }
        }
    }

    /** The Elite in hand, [forests] Forests to cycle it with, and the opponent's permanents. */
    private fun eliteGame(opposing: List<String>, forests: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Webstrike Elite")
            .withLandsOnBattlefield(1, "Forest", forests)
        opposing.forEach { builder.withCardOnBattlefield(2, it, summoningSickness = false) }
        repeat(8) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }
}
