package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Archpriest of Shadows (MOM #89) — {3}{B}{B} 4/4. Backup 1, deathtouch; on combat damage to a
 * player or battle, return target creature card from your graveyard to the battlefield.
 */
class ArchpriestOfShadowsScenarioTest : ScenarioTestBase() {
    init {
        context("Archpriest of Shadows") {

            test("combat damage to a player returns a creature card from your graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Archpriest of Shadows", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Archpriest of Shadows" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(game.findCardsInGraveyard(1, "Grizzly Bears").first())).error shouldBe null
                }
                game.resolveStack()
                withClue("Grizzly Bears returned") { (game.findPermanent("Grizzly Bears") != null) shouldBe true }
            }

            test("backup on another creature puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Archpriest of Shadows")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Archpriest of Shadows").error shouldBe null
                game.resolveStack()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                val counters = game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                counters shouldBe 1
            }
        }
    }
}
