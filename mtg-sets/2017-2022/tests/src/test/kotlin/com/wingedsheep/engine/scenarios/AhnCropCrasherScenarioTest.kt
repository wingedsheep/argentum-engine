package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Ahn-Crop Crasher (AKH #117).
 *
 *   Haste. You may exert this creature as it attacks. When you do, target creature can't block
 *   this turn.
 */
class AhnCropCrasherScenarioTest : ScenarioTestBase() {

    init {
        context("Ahn-Crop Crasher") {

            fun game() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Ahn-Crop Crasher")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("exerting stops the targeted creature from blocking; others still can") {
                val game = game()
                val crasher = game.findPermanent("Ahn-Crop Crasher")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ahn-Crop Crasher" to 2)).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(crasher))
                game.state.getEntity(crasher)!!.has<ExertedComponent>() shouldBe true

                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("the targeted Bears can't block") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Ahn-Crop Crasher"))).error shouldNotBe null
                }
                withClue("the untargeted Hill Giant still can") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Ahn-Crop Crasher"))).error shouldBe null
                }
            }

            test("declining the exert puts no trigger on the stack") {
                val game = game()
                val crasher = game.findPermanent("Ahn-Crop Crasher")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ahn-Crop Crasher" to 2)).error shouldBe null
                game.selectCards(emptyList())

                game.state.stack.isEmpty() shouldBe true
                game.state.getEntity(crasher)!!.has<ExertedComponent>() shouldBe false
            }
        }
    }
}
