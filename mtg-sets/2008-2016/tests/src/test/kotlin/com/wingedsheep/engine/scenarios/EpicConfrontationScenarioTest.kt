package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Epic Confrontation (DTK #185) — {1}{G} Sorcery.
 *
 *   Target creature you control gets +1/+2 until end of turn. It fights target creature you
 *   don't control.
 *
 * Pins that the pump lands before the fight: a 2/2 becomes 3/4, kills a 3/3 and survives its
 * 3 damage.
 */
class EpicConfrontationScenarioTest : ScenarioTestBase() {

    init {
        test("pumped creature fights, kills the opposing creature, and survives") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Epic Confrontation")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val spell = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Epic Confrontation"
            }

            game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant))
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.findPermanent("Grizzly Bears") shouldBe bears
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 4
            game.isInGraveyard(1, "Epic Confrontation") shouldBe true
        }
    }
}
