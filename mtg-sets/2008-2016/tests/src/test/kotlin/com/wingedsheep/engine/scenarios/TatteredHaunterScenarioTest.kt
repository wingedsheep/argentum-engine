package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tattered Haunter (EMN #77) — {1}{U} Creature — Spirit, 2/1.
 *
 *   Flying
 *   This creature can block only creatures with flying.
 *
 * The restriction is [com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith], enforced by the
 * engine's `CanOnlyBlockCreaturesWithRule` block-legality check.
 */
class TatteredHaunterScenarioTest : ScenarioTestBase() {

    init {
        test("can block a flying attacker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tattered Haunter")
                .withCardOnBattlefield(2, "Birds of Paradise") // flying
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            val haunter = game.findPermanent("Tattered Haunter")!!
            game.state.projectedState.hasKeyword(haunter, Keyword.FLYING) shouldBe true

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Birds of Paradise" to 1)).error shouldBe null

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            val result = game.declareBlockers(mapOf("Tattered Haunter" to listOf("Birds of Paradise")))
            withClue("blocking a flier is legal: ${result.error}") { result.error shouldBe null }
        }

        test("cannot block a non-flying attacker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tattered Haunter")
                .withCardOnBattlefield(2, "Hill Giant") // no flying
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            val result = game.declareBlockers(mapOf("Tattered Haunter" to listOf("Hill Giant")))
            withClue("blocking a non-flier is illegal: ${result.error}") { result.error shouldNotBe null }
        }
    }
}
