package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Confront the Unknown (SOI #198) — {G} Instant.
 *
 *   Investigate, then target creature gets +1/+1 until end of turn for each Clue you control.
 */
class ConfrontTheUnknownScenarioTest : ScenarioTestBase() {

    private val clueAbilityId get() = cardRegistry.getCard("Clue")!!.activatedAbilities.single().id

    private fun board(existingClues: Int) = scenario()
        .withPlayers("P1", "P2")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .apply { repeat(existingClues) { withCardOnBattlefield(1, "Clue", isToken = true) } }
        .withCardOnBattlefield(2, "Clue", isToken = true)
        .withCardInHand(1, "Confront the Unknown")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("the Clue it creates is counted, so with no Clues beforehand the creature gets +1/+1") {
            val game = board(existingClues = 0)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Confront the Unknown", bears).error shouldBe null
            game.resolveStack()

            withClue("one new Clue for P1, plus the opponent's Clue") {
                game.findPermanents("Clue") shouldHaveSize 2
            }
            withClue("only Clues P1 controls count") {
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
            }
        }

        test("counts existing Clues too, and the bonus stays locked after a Clue is sacrificed") {
            val game = board(existingClues = 2)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Confront the Unknown", bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.getToughness(bears) shouldBe 5

            val myClue = game.findPermanents("Clue").first { game.state.projectedState.getController(it) == game.player1Id }
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = myClue, abilityId = clueAbilityId)).error shouldBe null
            game.resolveStack()

            withClue("the amount was fixed as the spell resolved") {
                game.state.projectedState.getPower(bears) shouldBe 5
                game.state.projectedState.getToughness(bears) shouldBe 5
            }
        }
    }
}
