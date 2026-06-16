package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Fake Your Own Death (SNC, reprinted in OTJ).
 *
 * "Until end of turn, target creature gets +2/+0 and gains 'When this creature dies,
 * return it to the battlefield tapped under its owner's control and you create a
 * Treasure token.'"
 *
 * The card composes existing SDK primitives (ModifyStats +2/+0, a granted self
 * dies-trigger that returns the creature tapped from the graveyard and makes a
 * Treasure), so these tests confirm the composed behaviour resolves as the oracle
 * text reads — the buff applies, and on death the creature returns tapped with a
 * Treasure created.
 */
class FakeYourOwnDeathScenarioTest : ScenarioTestBase() {

    init {
        test("grants +2/+0 to the target creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Fake Your Own Death")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                .withLandsOnBattlefield(1, "Swamp", 2) // {1}{B}
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.castSpell(1, "Fake Your Own Death", bears)
            withClue("Casting Fake Your Own Death should succeed: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()

            withClue("Grizzly Bears should be 4/2 after +2/+0") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
        }

        test("when the buffed creature dies, it returns tapped under owner's control and a Treasure is created") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Fake Your Own Death")
                .withCardInHand(1, "Lightning Bolt")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                .withLandsOnBattlefield(1, "Swamp", 2) // {1}{B} for Fake Your Own Death
                .withLandsOnBattlefield(1, "Mountain", 1) // {R} for Lightning Bolt
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            // Grant the dies-trigger + buff.
            game.castSpell(1, "Fake Your Own Death", bears).error shouldBe null
            game.resolveStack()

            // Kill the now-4/2 creature with 3 damage. The granted dies-trigger fires.
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears should have returned to the battlefield (a new permanent)") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            val returned = game.findPermanent("Grizzly Bears")!!
            withClue("The returned Grizzly Bears should be tapped") {
                game.state.getEntity(returned)?.get<TappedComponent>() shouldNotBe null
            }
            withClue("The returned creature should be under its owner's (Player1) control") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }
            withClue("A Treasure token should have been created") {
                game.findPermanents("Treasure").isNotEmpty() shouldBe true
            }
            withClue("The +2/+0 buff does not carry to the returned creature (new object)") {
                game.state.projectedState.getPower(returned) shouldBe 2
                game.state.projectedState.getToughness(returned) shouldBe 2
            }
        }
    }
}
