package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Leyline Phantom (GTC) — "When this creature deals combat damage, return it to its owner's hand.
 * (Return it only if it survived combat.)"
 */
class LeylinePhantomScenarioTest : ScenarioTestBase() {
    init {
        test("returns to its owner's hand after dealing combat damage to a player") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Leyline Phantom")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Leyline Phantom" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 15
            game.isOnBattlefield("Leyline Phantom") shouldBe false
            game.isInHand(1, "Leyline Phantom") shouldBe true
        }

        test("stays in the graveyard when it dies in the combat it dealt damage in") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Leyline Phantom")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Leyline Phantom" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Craw Wurm" to listOf("Leyline Phantom"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            game.isInGraveyard(1, "Leyline Phantom") shouldBe true
            game.isInHand(1, "Leyline Phantom") shouldBe false
        }
    }
}
