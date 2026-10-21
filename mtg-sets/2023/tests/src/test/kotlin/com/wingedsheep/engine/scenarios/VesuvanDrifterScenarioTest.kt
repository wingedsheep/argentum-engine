package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vesuvan Drifter (MAT #10): at the beginning of each combat, you may reveal the top card of your
 * library; if it's a creature card, Drifter becomes a copy of it until end of turn, except it has
 * flying. The copy source stays in the library (`sourceFromAnyZone`).
 */
class VesuvanDrifterScenarioTest : ScenarioTestBase() {

    init {
        test("reveals a creature on top: becomes a copy of it with flying until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Vesuvan Drifter")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val drifter = game.findPermanents("Vesuvan Drifter").first()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()
            if (game.hasPendingDecision()) game.answerYesNo(true)
            game.resolveStack()

            withClue("Drifter became a copy of Grizzly Bears") {
                game.state.getEntity(drifter)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
                game.state.projectedState.getPower(drifter) shouldBe 2
                game.state.projectedState.getToughness(drifter) shouldBe 2
            }
            withClue("the copy keeps flying as a copy exception") {
                game.state.projectedState.hasKeyword(drifter, Keyword.FLYING) shouldBe true
            }
            withClue("the revealed card stays in the library") {
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            }
        }

        test("a noncreature card on top leaves Drifter unchanged") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Vesuvan Drifter")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val drifter = game.findPermanents("Vesuvan Drifter").first()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()
            if (game.hasPendingDecision()) game.answerYesNo(true)
            game.resolveStack()

            game.state.getEntity(drifter)?.get<CardComponent>()?.name shouldBe "Vesuvan Drifter"
            game.state.projectedState.getPower(drifter) shouldBe 2
            game.state.projectedState.getToughness(drifter) shouldBe 4
        }
    }
}
