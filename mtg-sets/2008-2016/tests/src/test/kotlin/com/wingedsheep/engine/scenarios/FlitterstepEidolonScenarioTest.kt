package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Flitterstep Eidolon: "This creature can't be blocked. Enchanted creature gets +1/+1 and can't be
 * blocked." The host's evasion must come from the Aura's attached-creature static, not the Eidolon's
 * own creature flag.
 */
class FlitterstepEidolonScenarioTest : ScenarioTestBase() {

    init {
        test("as a creature it can't be blocked") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Flitterstep Eidolon", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Flitterstep Eidolon" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Flitterstep Eidolon"))).error shouldNotBe null
        }

        test("bestowed, the host gets +1/+1 and can't be blocked") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Flitterstep Eidolon")
                .withLandsOnBattlefield(1, "Island", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Flitterstep Eidolon").single(),
                    targets = listOf(ChosenTarget.Permanent(giant)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(giant) shouldBe 4
            projected.getToughness(giant) shouldBe 4

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
        }
    }
}
