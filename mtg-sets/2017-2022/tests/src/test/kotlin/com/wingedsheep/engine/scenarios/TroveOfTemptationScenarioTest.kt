package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Trove of Temptation (XLN, reprinted in J22).
 *
 * "Each opponent must attack you or a planeswalker you control with at least one creature each
 * combat if able. At the beginning of your end step, create a Treasure token."
 */
class TroveOfTemptationScenarioTest : ScenarioTestBase() {

    init {
        test("an opponent with an able creature must attack the Trove's controller") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(2, "Trove of Temptation")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            withClue("declaring no attackers breaks the requirement") {
                game.declareAttackers(emptyMap()).error shouldNotBe null
            }
            withClue("one creature attacking the Trove's controller is enough") {
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            }
        }

        test("the Trove's controller is free to stay home on their own turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Trove of Temptation")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(emptyMap()).error shouldBe null
        }

        test("creates a Treasure at the beginning of its controller's end step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Trove of Temptation")
                .withActivePlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.findPermanents("Treasure") shouldHaveSize 0
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.findPermanents("Treasure") shouldHaveSize 1
        }
    }
}
