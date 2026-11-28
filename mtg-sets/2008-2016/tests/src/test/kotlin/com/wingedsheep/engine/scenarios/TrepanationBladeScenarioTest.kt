package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Trepanation Blade (ISD #235) — {3} Artifact — Equipment.
 *
 * "Whenever equipped creature attacks, defending player reveals cards from the top of their library
 * until they reveal a land card. The creature gets +1/+0 until end of turn for each card revealed
 * this way. That player puts the revealed cards into their graveyard. Equip {2}"
 *
 * Pins that the reveal walks the *defending* player's library, that the land itself counts toward
 * the bonus and goes to the graveyard (2017-11-17 ruling), and that with no land every card is
 * revealed, counted and milled.
 */
class TrepanationBladeScenarioTest : ScenarioTestBase() {

    private fun TestGame.attackWithBears() {
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        resolveStack()
    }

    init {
        test("reveals down to and including the first land; each revealed card adds +1/+0 and is milled") {
            // Opponent's library, top first: Hill Giant, Centaur Courser, Forest, Indomitable Ancients.
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Trepanation Blade", "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInLibrary(2, "Centaur Courser")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Indomitable Ancients")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.attackWithBears()

            withClue("three cards revealed (the Forest included): the 2/2 becomes 5/2") {
                game.state.projectedState.getPower(bears) shouldBe 5
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
            withClue("the revealed cards go to the defending player's graveyard; the rest stay") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isInGraveyard(2, "Centaur Courser") shouldBe true
                game.isInGraveyard(2, "Forest") shouldBe true
                game.graveyardSize(2) shouldBe 3
                game.librarySize(2) shouldBe 1
            }
            withClue("the attacker's own library is untouched") {
                game.librarySize(1) shouldBe 1
                game.graveyardSize(1) shouldBe 0
            }
        }

        test("a land on top reveals just that land for +1/+0") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Trepanation Blade", "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.attackWithBears()

            game.state.projectedState.getPower(bears) shouldBe 3
            game.isInGraveyard(2, "Forest") shouldBe true
            game.librarySize(2) shouldBe 1
        }

        test("with no land, every card is revealed, counted and put into the graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Trepanation Blade", "Grizzly Bears")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInLibrary(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.attackWithBears()

            game.state.projectedState.getPower(bears) shouldBe 4
            game.graveyardSize(2) shouldBe 2
            game.librarySize(2) shouldBe 0
        }

        test("the bonus lasts only until end of turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Trepanation Blade", "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.attackWithBears()
            game.state.projectedState.getPower(bears) shouldBe 3

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.projectedState.getPower(bears) shouldBe 2
        }
    }
}
