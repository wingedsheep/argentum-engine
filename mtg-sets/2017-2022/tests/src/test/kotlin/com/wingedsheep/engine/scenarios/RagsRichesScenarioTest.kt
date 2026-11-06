package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rags // Riches (AKH #222).
 *
 * Riches {5}{U}{U}, Aftermath: "Each opponent chooses a creature they control. You gain control of
 * those creatures."
 *
 * The opponent — not the caster — makes the choice among their own creatures, and the steal that
 * follows the per-opponent loop must land with the caster (the spell's controller), permanently.
 */
class RagsRichesScenarioTest : ScenarioTestBase() {

    init {
        test("Riches from the graveyard: the opponent chooses one of their creatures and the caster gains control of it") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withLandsOnBattlefield(1, "Island", 7)
                .withCardInGraveyard(1, "Rags // Riches")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

            val inGraveyard = game.findCardsInGraveyard(1, "Rags // Riches").first()
            game.execute(CastSpell(game.player1Id, inGraveyard, faceIndex = 1)).error.shouldBeNull()
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the opponent chooses, among the creatures they control") {
                decision.playerId shouldBe game.player2Id
                decision.options shouldContainExactlyInAnyOrder listOf(giant, bears)
            }
            game.selectCards(listOf(giant)).error.shouldBeNull()
            game.resolveStack()

            withClue("the caster gains control of the chosen creature") {
                game.state.projectedState.getController(giant) shouldBe game.player1Id
            }
            withClue("the unchosen creature stays with the opponent") {
                game.state.projectedState.getController(bears) shouldBe game.player2Id
            }
            withClue("Riches, cast from the graveyard, is exiled") {
                game.isInExile(1, "Rags // Riches") shouldBe true
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the control change is permanent — it survives the cleanup step") {
                game.state.projectedState.getController(giant) shouldBe game.player1Id
            }
        }
    }
}
