package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Dance with Devils (SOI #150) — two 1/1 Devil tokens, each with a *targeted* granted
 * "when this token dies, it deals 1 damage to any target". Proves the minted token's dies
 * trigger asks for its target and lands the damage.
 */
class DanceWithDevilsScenarioTest : ScenarioTestBase() {
    init {
        test("a dying Devil token deals 1 damage to the chosen target") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Dance with Devils")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Dance with Devils").error shouldBe null
            game.resolveStack()
            val devils = game.findPermanents("Devil Token").ifEmpty { game.findPermanents("Devil") }
            devils shouldHaveSize 2

            game.castSpell(1, "Shock", devils.first()).error shouldBe null
            game.resolveStack()

            withClue("pending=${game.state.pendingDecision}") {
                (game.state.pendingDecision is ChooseTargetsDecision) shouldBe true
            }
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            withClue("life2=${game.getLifeTotal(2)} stack=${game.state.stack.size}") {
                game.getLifeTotal(2) shouldBe 19
                (game.findPermanents("Devil Token") + game.findPermanents("Devil")) shouldHaveSize 1
            }
        }
    }
}
