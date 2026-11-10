package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Walker of the Wastes — the first static P/T bonus counted by land *name*. Only lands named
 * Wastes that you control count: not other basics, not an opponent's Wastes, and the bonus
 * follows the count as more Wastes arrive.
 */
class WalkerOfTheWastesScenarioTest : ScenarioTestBase() {
    init {
        test("gets +1/+1 for each Wastes you control, ignoring other lands and an opponent's Wastes") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Walker of the Wastes")
                .withLandsOnBattlefield(1, "Wastes", 2)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(2, "Wastes", 4)
                .withCardInHand(1, "Wastes")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val walker = game.findPermanent("Walker of the Wastes")!!

            withClue("two Wastes you control: 4/4 + 2/2") {
                game.state.projectedState.getPower(walker) shouldBe 6
                game.state.projectedState.getToughness(walker) shouldBe 6
            }

            val wastes = game.findCardsInHand(1, "Wastes").first()
            withClue("playing Wastes is legal") {
                game.execute(PlayLand(game.player1Id, wastes)).error shouldBe null
            }

            withClue("a third Wastes raises the bonus to +3/+3") {
                game.state.projectedState.getPower(walker) shouldBe 7
                game.state.projectedState.getToughness(walker) shouldBe 7
            }
        }
    }
}
