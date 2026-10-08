package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Loran's Escape (BRO #14) — target artifact or creature gains hexproof and indestructible until
 * end of turn, then scry 1. Exercised on a noncreature artifact (the "artifact or" half of the
 * target) and on a creature.
 */
class LoransEscapeScenarioTest : ScenarioTestBase() {

    private fun protect(targetName: String) {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Loran's Escape")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardOnBattlefield(1, targetName)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val target = game.findPermanent(targetName)!!
        game.castSpell(1, "Loran's Escape", target).error shouldBe null
        game.resolveStack()

        withClue("Scry 1 pauses for the top-card selection") {
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        }
        game.skipSelection()

        val projected = game.state.projectedState
        withClue("$targetName gains hexproof and indestructible") {
            projected.hasKeyword(target, Keyword.HEXPROOF) shouldBe true
            projected.hasKeyword(target, Keyword.INDESTRUCTIBLE) shouldBe true
        }
    }

    init {
        test("a noncreature artifact gains hexproof and indestructible, then scry 1") {
            protect("Sol Ring")
        }

        test("a creature gains hexproof and indestructible, then scry 1") {
            protect("Grizzly Bears")
        }
    }
}
