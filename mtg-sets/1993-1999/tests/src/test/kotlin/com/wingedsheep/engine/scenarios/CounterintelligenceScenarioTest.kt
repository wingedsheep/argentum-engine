package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class CounterintelligenceScenarioTest : ScenarioTestBase() {
    init {
        test("returns two target creatures to their owners' hands") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Counterintelligence")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Counterintelligence").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val result = game.execute(
                CastSpell(
                    game.player1Id, spell,
                    listOf(entityIdToChosenTarget(game.state, bears), entityIdToChosenTarget(game.state, giant)),
                )
            )
            result.error shouldBe null
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldBe null
            game.findPermanent("Hill Giant") shouldBe null
            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
            game.findCardsInHand(2, "Hill Giant").size shouldBe 1
        }

        test("returns a single target creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Counterintelligence")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Counterintelligence", giant).error shouldBe null
            game.resolveStack()
            game.findPermanent("Hill Giant") shouldBe null
            (game.findPermanent("Grizzly Bears") != null) shouldBe true
            game.findCardsInHand(2, "Hill Giant").size shouldBe 1
        }
    }
}
