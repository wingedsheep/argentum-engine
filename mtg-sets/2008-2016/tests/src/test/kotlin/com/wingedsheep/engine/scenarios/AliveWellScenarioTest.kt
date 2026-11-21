package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class AliveWellScenarioTest : ScenarioTestBase() {
    init {
        fun board(forests: Int = 4, plains: Int = 1): TestGame = scenario()
            .withPlayers("Caster", "Opponent").withCardInHand(1, "Alive // Well")
            .withLandsOnBattlefield(1, "Forest", forests).withLandsOnBattlefield(1, "Plains", plains)
            .withCardOnBattlefield(1, "Grizzly Bears").withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        fun TestGame.cardId() = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Alive // Well" }

        test("fused, the Centaur from Alive counts toward Well's life gain") {
            val game = board(); val id = game.cardId()
            game.execute(CastSpell(game.player1Id, id, castFused = true)).error.shouldBeNull()
            game.resolveStack()
            game.findPermanents("Centaur Token") shouldHaveSize 1
            // Grizzly Bears + Centaur = 2 creatures you control; the opponent's Bears don't count.
            game.getLifeTotal(1) shouldBe 24
        }
        test("Well alone counts only creatures you control") {
            val game = board(0, 1); val id = game.cardId()
            game.execute(CastSpell(game.player1Id, id, faceIndex = 1)).error.shouldBeNull()
            game.resolveStack()
            game.findPermanents("Centaur Token").shouldBeEmpty()
            game.getLifeTotal(1) shouldBe 22
        }
        test("Alive alone creates a 3/3 green Centaur and gains no life") {
            val game = board(4, 0); val id = game.cardId()
            game.execute(CastSpell(game.player1Id, id, faceIndex = 0)).error.shouldBeNull()
            game.resolveStack()
            game.findPermanents("Centaur Token") shouldHaveSize 1
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
