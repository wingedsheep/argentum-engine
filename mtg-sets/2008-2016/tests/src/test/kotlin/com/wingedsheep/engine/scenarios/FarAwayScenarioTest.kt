package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class FarAwayScenarioTest : ScenarioTestBase() {
    init {
        test("fused, Far bounces first so Away's sacrifice must take the other creature") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Far // Away")
                .withLandsOnBattlefield(1, "Island", 2).withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(2, "Grizzly Bears").withCardOnBattlefield(2, "Centaur Courser")
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Far // Away"
            }

            game.execute(
                CastSpell(
                    game.player1Id, cardId,
                    targets = listOf(
                        ChosenTarget.Permanent(game.findPermanent("Centaur Courser")!!),
                        ChosenTarget.Player(game.player2Id)
                    ),
                    castFused = true
                )
            ).error.shouldBeNull()
            game.resolveStack()

            // Far returned the Courser before Away resolved, leaving the Bears as the only creature to sacrifice.
            game.isInHand(2, "Centaur Courser") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.findPermanent("Grizzly Bears").shouldBeNull()
            game.isInGraveyard(1, "Far // Away") shouldBe true
        }
    }
}
