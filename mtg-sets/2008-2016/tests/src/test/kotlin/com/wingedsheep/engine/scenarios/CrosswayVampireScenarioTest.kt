package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Crossway Vampire (ISD #135) — {1}{R}{R} 3/2 Vampire.
 *
 *   When this creature enters, target creature can't block this turn.
 */
class CrosswayVampireScenarioTest : ScenarioTestBase() {

    init {
        test("the targeted creature can't block this turn; other creatures still can") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Crossway Vampire")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val cast = game.castSpell(1, "Crossway Vampire")
            withClue("Crossway Vampire should cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("Grizzly Bears was targeted and can't block") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
            }
            withClue("Centaur Courser wasn't targeted and may block") {
                game.declareBlockers(mapOf("Centaur Courser" to listOf("Hill Giant"))).error shouldBe null
            }
        }
    }
}
