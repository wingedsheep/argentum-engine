package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class EldraziMimicScenarioTest : ScenarioTestBase() {
    init {
        fun mimicBoard(vararg hand: String) = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Eldrazi Mimic")
            .also { b -> hand.forEach { b.withCardInHand(1, it) } }
            .withLandsOnBattlefield(1, "Mountain", 5)
            .withLandsOnBattlefield(1, "Forest", 1)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("a devoid creature entering lets Mimic copy its power and toughness until end of turn") {
            val game = mimicBoard("Eldrazi Aggressor")
            val mimic = game.findPermanent("Eldrazi Mimic")!!

            game.castSpell(1, "Eldrazi Aggressor").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldNotBe null
            game.answerYesNo(true)
            game.resolveStack()

            withClue("Mimic's base P/T became the Aggressor's 2/3") {
                game.state.projectedState.getPower(mimic) shouldBe 2
                game.state.projectedState.getToughness(mimic) shouldBe 3
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the effect ends at end of turn") {
                game.state.projectedState.getPower(mimic) shouldBe 2
                game.state.projectedState.getToughness(mimic) shouldBe 1
            }
        }

        test("reads the creature's power and toughness on resolution, using last-known values once it is gone") {
            val game = mimicBoard("Eldrazi Aggressor", "Brute Strength", "Go for the Throat")
            val mimic = game.findPermanent("Eldrazi Mimic")!!

            game.castSpell(1, "Eldrazi Aggressor").error shouldBe null
            game.passPriority()
            game.passPriority()
            val aggressor = game.findPermanent("Eldrazi Aggressor")!!
            game.state.stack.size shouldBe 1 // Mimic's trigger

            // P1 pumps the Aggressor in response: +3/+1 makes it 5/4.
            game.castSpell(1, "Brute Strength", aggressor).error shouldBe null
            game.passPriority()
            game.passPriority()
            game.state.projectedState.getPower(aggressor) shouldBe 5

            // Then it is destroyed while the trigger is still waiting.
            game.castSpell(1, "Go for the Throat", aggressor).error shouldBe null
            game.passPriority()
            game.passPriority()
            game.isInGraveyard(1, "Eldrazi Aggressor") shouldBe true
            game.state.stack.size shouldBe 1

            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            withClue("Mimic took the Aggressor's last-known 5/4") {
                game.state.projectedState.getPower(mimic) shouldBe 5
                game.state.projectedState.getToughness(mimic) shouldBe 4
            }
        }

        test("colored creatures don't trigger it, and declining keeps Mimic a 2/1") {
            val game = mimicBoard("Grizzly Bears", "Hedron Crawler")
            val mimic = game.findPermanent("Eldrazi Mimic")!!

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("a green creature entering is not colorless") {
                game.state.pendingDecision shouldBe null
                game.state.stack.size shouldBe 0
            }

            game.castSpell(1, "Hedron Crawler").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldNotBe null
            game.answerYesNo(false)
            game.resolveStack()

            game.state.projectedState.getPower(mimic) shouldBe 2
            game.state.projectedState.getToughness(mimic) shouldBe 1
        }
    }
}
