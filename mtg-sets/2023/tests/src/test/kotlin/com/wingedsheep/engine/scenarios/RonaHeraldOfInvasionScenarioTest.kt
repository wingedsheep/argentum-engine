package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rona, Herald of Invasion // Rona, Tolarian Obliterator (MOM #75).
 *
 *   Front — untaps when you cast a legendary spell; "{T}: Draw a card, then discard a card.";
 *           "{5}{B/P}: Transform Rona. Activate only as a sorcery."
 *   Back  — 5/5 trample; the damage-source's controller exiles a random card from hand.
 */
class RonaHeraldOfInvasionScenarioTest : ScenarioTestBase() {

    private val transformAbility get() = cardRegistry.getCard("Rona, Herald of Invasion")!!.activatedAbilities[1].id

    init {
        context("Rona, Herald of Invasion") {
            test("transforms into a 5/5 trampler") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rona, Herald of Invasion", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rona = game.findPermanent("Rona, Herald of Invasion")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = rona, abilityId = transformAbility)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.state.getEntity(rona)!!.get<CardComponent>()!!.name shouldBe "Rona, Tolarian Obliterator"
                game.state.projectedState.hasKeyword(rona, Keyword.TRAMPLE) shouldBe true
                game.state.projectedState.getPower(rona) shouldBe 5
            }
        }
    }
}
