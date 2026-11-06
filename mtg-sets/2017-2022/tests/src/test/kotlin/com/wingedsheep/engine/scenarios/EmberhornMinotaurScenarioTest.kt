package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Emberhorn Minotaur (AKH #130).
 *
 *   You may exert this creature as it attacks. When you do, it gets +1/+1 and gains menace until
 *   end of turn.
 */
class EmberhornMinotaurScenarioTest : ScenarioTestBase() {

    init {
        context("Emberhorn Minotaur") {

            fun game() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Emberhorn Minotaur")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("exerting makes it 5/4 with menace before blockers") {
                val game = game()
                val minotaur = game.findPermanent("Emberhorn Minotaur")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Emberhorn Minotaur" to 2)).error shouldBe null
                game.selectCards(listOf(minotaur))
                game.state.getEntity(minotaur)!!.has<ExertedComponent>() shouldBe true
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(minotaur) shouldBe 5
                projected.getToughness(minotaur) shouldBe 4
                projected.hasKeyword(minotaur, Keyword.MENACE) shouldBe true

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("a single blocker is illegal against menace") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Emberhorn Minotaur"))).error shouldNotBe null
                }
                withClue("two blockers may block it") {
                    game.declareBlockers(
                        mapOf(
                            "Grizzly Bears" to listOf("Emberhorn Minotaur"),
                            "Hill Giant" to listOf("Emberhorn Minotaur"),
                        )
                    ).error shouldBe null
                }
            }

            test("declining the exert leaves it a 4/3 without menace") {
                val game = game()
                val minotaur = game.findPermanent("Emberhorn Minotaur")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Emberhorn Minotaur" to 2)).error shouldBe null
                game.selectCards(emptyList())

                game.state.stack.isEmpty() shouldBe true
                game.state.getEntity(minotaur)!!.has<ExertedComponent>() shouldBe false
                val projected = game.state.projectedState
                projected.getPower(minotaur) shouldBe 4
                projected.hasKeyword(minotaur, Keyword.MENACE) shouldBe false
            }
        }
    }
}
