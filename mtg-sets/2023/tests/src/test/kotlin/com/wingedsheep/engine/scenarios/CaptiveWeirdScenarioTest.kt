package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Captive Weird // Compleated Conjurer (MOM #49).
 *
 *   Front — 1/3 defender; "{3}{R/P}: Transform. Activate only as a sorcery."
 *   Back  — 3/3; on transforming, exile the top card of your library and may play it until end of next turn.
 */
class CaptiveWeirdScenarioTest : ScenarioTestBase() {

    private val transformAbility get() = cardRegistry.getCard("Captive Weird")!!.activatedAbilities[0].id

    init {
        context("Captive Weird") {
            test("transforms into a 3/3 and exiles the top card of the library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Captive Weird", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val weird = game.findPermanent("Captive Weird")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = weird, abilityId = transformAbility)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.state.getEntity(weird)!!.get<CardComponent>()!!.name shouldBe "Compleated Conjurer"
                game.state.projectedState.getPower(weird) shouldBe 3
                game.librarySize(1) shouldBe 0
            }
        }
    }
}
