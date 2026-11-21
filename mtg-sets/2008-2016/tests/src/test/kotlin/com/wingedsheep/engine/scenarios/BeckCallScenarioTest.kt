package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BeckCallScenarioTest : ScenarioTestBase() {
    init {
        fun board(): TestGame {
            val builder = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Beck // Call")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 3).withLandsOnBattlefield(1, "Island", 4)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(2, "Plains")
            repeat(6) { builder.withCardInLibrary(1, "Plains") }
            return builder.withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        }
        fun TestGame.cardId() = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Beck // Call" }

        test("fused, Beck's trigger sees all four Birds enter — up to four draws") {
            val game = board(); val id = game.cardId()
            val handBefore = game.handSize(1)
            game.execute(CastSpell(game.player1Id, id, castFused = true)).error.shouldBeNull()
            game.resolveStack()
            game.findPermanents("Bird Token") shouldHaveSize 4
            var prompts = 0
            while (game.state.pendingDecision != null) {
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                // Accept three, decline one: each draw is optional.
                game.answerYesNo(prompts < 3).error.shouldBeNull()
                prompts++
                game.resolveStack()
            }
            prompts shouldBe 4
            // Beck // Call left the hand; three accepted draws.
            game.handSize(1) shouldBe handBefore - 1 + 3
        }
        test("Beck alone: a creature entering later this turn offers a draw") {
            val game = board(); val id = game.cardId()
            game.execute(CastSpell(game.player1Id, id, faceIndex = 0)).error.shouldBeNull()
            game.resolveStack()
            game.findPermanents("Bird Token").shouldBeEmpty()
            game.castSpell(1, "Grizzly Bears").error.shouldBeNull()
            game.resolveStack()
            val handBefore = game.handSize(1)
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error.shouldBeNull()
            game.resolveStack()
            game.handSize(1) shouldBe handBefore + 1
        }
        test("Call alone creates four 1/1 flying Birds and draws nothing") {
            val game = board(); val id = game.cardId()
            val handBefore = game.handSize(1)
            game.execute(CastSpell(game.player1Id, id, faceIndex = 1)).error.shouldBeNull()
            game.resolveStack()
            game.findPermanents("Bird Token") shouldHaveSize 4
            game.state.pendingDecision.shouldBeNull()
            game.handSize(1) shouldBe handBefore - 1
        }
    }
}
