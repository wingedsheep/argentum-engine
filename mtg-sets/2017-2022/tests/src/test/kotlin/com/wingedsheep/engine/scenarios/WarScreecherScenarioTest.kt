package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.WarScreecher
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * War Screecher (WAR #39) — {1}{W} Creature — Bird 1/3, flying.
 *
 *   {5}{W}, {T}: Other creatures you control get +1/+1 until end of turn.
 */
class WarScreecherScenarioTest : ScenarioTestBase() {

    init {
        test("the ability pumps your other creatures, not itself or the opponent's") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "War Screecher")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val screecher = game.findPermanent("War Screecher")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                ActivateAbility(game.player1Id, screecher, WarScreecher.activatedAbilities[0].id)
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            game.state.getEntity(screecher)!!.has<TappedComponent>() shouldBe true
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.getPower(screecher) shouldBe 1
            projected.getToughness(screecher) shouldBe 3
            projected.getPower(giant) shouldBe 3
        }
    }
}
