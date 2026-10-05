package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Press for Answers (SOI #80) — {1}{U} Sorcery.
 *
 * "Tap target creature. It doesn't untap during its controller's next untap step. Investigate."
 *
 * Covers the tap, the skipped untap (only the *next* one), the Clue, and the ruling that an
 * illegal target on resolution means no investigate.
 */
class PressForAnswersScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Press for Answers")
        .withCardInHand(1, "Lightning Bolt")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.has<TappedComponent>() ?: false

    init {
        test("taps the creature, it skips its controller's next untap only, and you investigate") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Press for Answers", giant).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant is tapped") { game.isTapped(giant) shouldBe true }
            withClue("one Clue was created") { game.findPermanents("Clue").size shouldBe 1 }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("opponent's upkeep: Hill Giant stayed tapped through its controller's untap step") {
                game.state.activePlayerId shouldBe game.player2Id
                game.isTapped(giant) shouldBe true
            }

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("opponent's following upkeep: the effect has expired and Hill Giant untapped") {
                game.state.activePlayerId shouldBe game.player2Id
                game.isTapped(giant) shouldBe false
            }
        }

        test("if the target becomes illegal, the spell doesn't resolve and there is no Clue") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Press for Answers", giant).error shouldBe null
            game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant died to Lightning Bolt before Press for Answers resolved") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
            withClue("the spell didn't resolve — no investigate") {
                game.findPermanents("Clue").size shouldBe 0
            }
            game.isInGraveyard(1, "Press for Answers") shouldBe true
        }
    }
}
