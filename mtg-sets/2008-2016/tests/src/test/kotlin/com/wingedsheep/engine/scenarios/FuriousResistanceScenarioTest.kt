package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Furious Resistance (GTC) — target blocking creature gets +3/+0 and gains first strike until end of turn. */
class FuriousResistanceScenarioTest : ScenarioTestBase() {
    init {
        test("pumps a blocker and gives it first strike") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Furious Resistance")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
            if (game.state.priorityPlayerId == game.player2Id) game.passPriority() // hand priority to the defender

            game.castSpell(1, "Furious Resistance", bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.getToughness(bears) shouldBe 2
            game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
        }

        test("can't target a creature that isn't blocking") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Furious Resistance")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Furious Resistance", game.findPermanent("Grizzly Bears")).error shouldNotBe null
        }
    }
}
