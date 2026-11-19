package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Siren Song Lyre (BNG #161) — "Equipped creature has \"{2}, {T}: Tap target creature.\" Equip {2}" */
class SirenSongLyreScenarioTest : ScenarioTestBase() {
    init {
        test("the equipped creature gains {2}, {T}: tap target creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Siren Song Lyre", "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            val granted = game.getLegalActions(1)
                .mapNotNull { it.action as? ActivateAbility }
                .first { it.sourceId == bears }
            game.execute(
                ActivateAbility(game.player1Id, bears, granted.abilityId, listOf(ChosenTarget.Permanent(giant)))
            ).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Hill Giant is tapped, and so is the bearer for {T}") {
                game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
