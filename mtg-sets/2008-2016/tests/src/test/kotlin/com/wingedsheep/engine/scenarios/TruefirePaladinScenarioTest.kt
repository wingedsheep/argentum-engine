package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Truefire Paladin (GTC) — Vigilance; {R}{W}: +2/+0 until end of turn;
 * {R}{W}: gains first strike until end of turn.
 */
class TruefirePaladinScenarioTest : ScenarioTestBase() {
    init {
        test("each {R}{W} ability pumps itself: +2/+0 and first strike") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Truefire Paladin")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val paladin = game.findPermanent("Truefire Paladin")!!
            val (pump, firstStrike) = cardRegistry.getCard("Truefire Paladin")!!.script.activatedAbilities.map { it.id }

            withClue("vigilance printed, no first strike yet") {
                game.state.projectedState.hasKeyword(paladin, Keyword.VIGILANCE) shouldBe true
                game.state.projectedState.hasKeyword(paladin, Keyword.FIRST_STRIKE) shouldBe false
            }

            val r1 = game.execute(ActivateAbility(game.player1Id, paladin, pump))
            withClue("pump activation should succeed: ${r1.error}") { r1.error shouldBe null }
            game.resolveStack()

            val r2 = game.execute(ActivateAbility(game.player1Id, paladin, firstStrike))
            withClue("first strike activation should succeed: ${r2.error}") { r2.error shouldBe null }
            game.resolveStack()

            withClue("4/2 with first strike") {
                game.state.projectedState.getPower(paladin) shouldBe 4
                game.state.projectedState.getToughness(paladin) shouldBe 2
                game.state.projectedState.hasKeyword(paladin, Keyword.FIRST_STRIKE) shouldBe true
            }
        }
    }
}
