package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Glorybringer (AKH #134).
 *
 *   Flying, haste. You may exert this creature as it attacks. When you do, it deals 4 damage to
 *   target non-Dragon creature an opponent controls.
 */
class GlorybringerScenarioTest : ScenarioTestBase() {

    init {
        context("Glorybringer") {

            fun game() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Glorybringer")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Shivan Dragon")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("exerting deals 4 to a non-Dragon creature an opponent controls; Dragons and own creatures aren't legal") {
                val game = game()
                val glorybringer = game.findPermanent("Glorybringer")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glorybringer" to 2)).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(glorybringer))
                game.state.getEntity(glorybringer)!!.has<ExertedComponent>() shouldBe true

                val targeting = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("only the opponent's non-Dragon creatures are legal targets") {
                    targeting.legalTargets[0]!!.shouldContainExactlyInAnyOrder(bears, giant)
                }

                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Shivan Dragon") shouldBe true
                game.isOnBattlefield("Craw Wurm") shouldBe true
            }

            test("declining the exert puts no trigger on the stack") {
                val game = game()
                val glorybringer = game.findPermanent("Glorybringer")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glorybringer" to 2)).error shouldBe null
                game.selectCards(emptyList())

                game.state.stack.isEmpty() shouldBe true
                game.state.getEntity(glorybringer)!!.has<ExertedComponent>() shouldBe false
            }
        }
    }
}
