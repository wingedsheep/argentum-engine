package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Elrond, Lord of Rivendell (LTR #49, {2}{U}) — "Whenever Elrond or another creature you control
 * enters, scry 1. If this is the second time this ability has resolved this turn, the Ring tempts
 * you."
 *
 * Regression: the ability must increment its per-ability resolution count so the `count == 2`
 * conditional fires on the second resolution. Without the IncrementAbilityResolutionCountEffect the
 * count stayed 0 and the Ring never tempted (cf. Tannuk, Memorial Ensign / Harvestrite Host).
 *
 * Libraries are left empty so scry 1 is a silent no-op (nothing to look at, no decision), isolating
 * the temptation as the only player decision.
 */
class ElrondLordOfRivendellScenarioTest : ScenarioTestBase() {

    init {
        test("the Ring tempts you on the second creature to enter in a turn, not the first") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Elrond, Lord of Rivendell", summoningSickness = false)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun temptCount(player: EntityId): Int =
                game.state.getEntity(player)?.get<TheRingComponent>()?.temptCount ?: 0

            // First creature enters: Elrond's ability resolves once → scry only, no temptation.
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            temptCount(game.player1Id) shouldBe 0

            // A second creature enters this same turn: the ability resolves a second time, so the
            // Ring tempts you — pick a Ring-bearer to complete the temptation.
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            val bearer = game.findPermanent("Grizzly Bears")!!
            game.selectCards(listOf(bearer))
            game.resolveStack()

            temptCount(game.player1Id) shouldBe 1
        }
    }
}
