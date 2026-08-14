package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Goblin-town Flunkies (HOB) — {1}{R} Creature — Goblin Soldier 1/1.
 *
 * "Haste
 *  When this creature enters, amass Goblins 1."
 *
 * With no Army in play the amass has to mint the 0/0 black Goblin Army token first and then put
 * the counter on it, so a fresh board yields a 1/1 Army. A second Flunky amasses onto that same
 * Army rather than making a second one.
 */
class GoblinTownFlunkiesScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin-town Flunkies") {

            test("it is a 1/1 with haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin-town Flunkies")
                    .build()

                val flunkies = game.findPermanent("Goblin-town Flunkies")!!
                game.state.projectedState.getPower(flunkies) shouldBe 1
                game.state.projectedState.getToughness(flunkies) shouldBe 1
                game.state.projectedState.hasKeyword(flunkies, Keyword.HASTE) shouldBe true
            }

            test("its ETB amasses Goblins 1, creating a 1/1 Goblin Army from nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Goblin-town Flunkies")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.findAllPermanents("Goblin Army").size shouldBe 0

                game.castSpell(1, "Goblin-town Flunkies").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val army = game.findPermanent("Goblin Army")
                    ?: error("amass should have created a Goblin Army token")
                withClue("a 0/0 token plus one +1/+1 counter") {
                    game.state.projectedState.getPower(army) shouldBe 1
                    game.state.projectedState.getToughness(army) shouldBe 1
                }
            }

            test("a second Flunky amasses onto the existing Army instead of making another") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardsInHand(1, "Goblin-town Flunkies", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                repeat(2) {
                    game.castSpell(1, "Goblin-town Flunkies").error shouldBe null
                    if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                    game.resolveStack()
                }

                withClue("amass never makes a second Army while one is already there") {
                    game.findAllPermanents("Goblin Army").size shouldBe 1
                }
                val army = game.findPermanent("Goblin Army")!!
                game.state.projectedState.getPower(army) shouldBe 2
                game.state.projectedState.getToughness(army) shouldBe 2
            }
        }
    }
}
