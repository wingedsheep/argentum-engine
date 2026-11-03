package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Majestic Heliopterus (RIX #13) — "Flying. Whenever this creature attacks, another target
 * Dinosaur you control gains flying until end of turn."
 */
class MajesticHeliopterusScenarioTest : ScenarioTestBase() {

    init {
        test("attacking gives another Dinosaur you control flying until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Majestic Heliopterus")
                .withCardOnBattlefield(1, "Ravenous Daggertooth")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val dino = game.findPermanent("Ravenous Daggertooth")!!
            game.state.projectedState.hasKeyword(dino, Keyword.FLYING) shouldBe false

            game.declareAttackers(mapOf("Majestic Heliopterus" to 2)).error shouldBe null
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets.values.flatten().toSet() shouldBe setOf(dino)
            game.selectTargets(listOf(dino)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(dino, Keyword.FLYING) shouldBe true
        }
    }
}
