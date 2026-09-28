package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invasion of Amonkhet // Lazotep Convert.
 *
 * Front: each player mills three, then each opponent discards a card and you draw one. Back: an
 * optional graveyard-sourced copy that is a 4/4 black Zombie *in addition to* the copied card's own
 * colours and types — the `EntersAsCopy.additionalColors` axis.
 */
class InvasionOfAmonkhetScenarioTest : ScenarioTestBase() {

    private fun TestGame.defeatSiegeAndCastBack() {
        checkStateBasedActions()
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackersWithPermanentTargets(
            permanentAttackers = mapOf("Serra Angel" to "Invasion of Amonkhet")
        ).error shouldBe null
        var guard = 0
        while (state.pendingDecision == null && guard++ < 30) {
            if (state.step == Step.DECLARE_BLOCKERS &&
                state.getEntity(player2Id)
                    ?.has<BlockersDeclaredThisCombatComponent>() != true
            ) {
                declareNoBlockers()
            } else {
                passPriority()
            }
        }
        answerYesNo(true).error shouldBe null
        resolveStack()
    }

    private fun backFaceScenario() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Invasion of Amonkhet")
        .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
        .withCardInGraveyard(2, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("front: each player mills three, then each opponent discards and you draw") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Amonkhet")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(2, "Grizzly Bears")
                .apply { repeat(4) { withCardInLibrary(1, "Island") } }
                .apply { repeat(4) { withCardInLibrary(2, "Swamp") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Amonkhet").error shouldBe null
            game.resolveStack()
            (game.getPendingDecision() as? SelectCardsDecision)?.let { d ->
                game.selectCards(d.options.take(1)).error shouldBe null
            }
            game.resolveStack()

            withClue("both players milled three") {
                game.librarySize(2) shouldBe 1
                game.graveyardSize(1) shouldBe 3
            }
            withClue("the opponent discarded their only card") {
                game.handSize(2) shouldBe 0
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
            withClue("you drew the last card of your library") {
                game.handSize(1) shouldBe 1
                game.librarySize(1) shouldBe 0
            }
        }

        test("back: Lazotep Convert copies a graveyard creature card as a 4/4 black Zombie in addition") {
            val game = backFaceScenario()
            game.defeatSiegeAndCastBack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            val copy = game.findPermanent("Grizzly Bears")
            copy shouldNotBe null
            val projected = game.state.projectedState
            withClue("the copy keeps the copied name and is forced to 4/4") {
                projected.getPower(copy!!) shouldBe 4
                projected.getToughness(copy) shouldBe 4
            }
            withClue("black is added to the copied green, Zombie to the copied Bear") {
                projected.hasColor(copy!!, Color.GREEN) shouldBe true
                projected.hasColor(copy, Color.BLACK) shouldBe true
                projected.hasSubtype(copy, "Bear") shouldBe true
                projected.hasSubtype(copy, "Zombie") shouldBe true
            }
            withClue("unlike Superior Spider-Man, the copied card stays in the graveyard") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }

        test("back: declining the copy leaves Lazotep Convert as its printed 4/4 Zombie") {
            val game = backFaceScenario()
            game.defeatSiegeAndCastBack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            val convert = game.findPermanent("Lazotep Convert")
            convert shouldNotBe null
            game.state.projectedState.getPower(convert!!) shouldBe 4
            game.state.projectedState.hasSubtype(convert, "Zombie") shouldBe true
        }
    }
}
