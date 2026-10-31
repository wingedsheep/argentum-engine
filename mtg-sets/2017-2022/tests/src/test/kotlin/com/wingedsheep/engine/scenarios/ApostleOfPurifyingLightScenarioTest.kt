package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Apostle of Purifying Light (M20 #6) — {1}{W} 2/1 Creature — Human Cleric.
 *
 *   Protection from black
 *   {2}: Exile target card from a graveyard.
 *
 * The activated ability targets a card in any graveyard and exiles it; it has no tap cost, so it
 * can be activated repeatedly.
 */
class ApostleOfPurifyingLightScenarioTest : ScenarioTestBase() {

    init {
        context("Apostle of Purifying Light") {
            test("{2}: exiles a card from an opponent's graveyard, then one from its controller's") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Apostle of Purifying Light")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val apostle = game.findPermanent("Apostle of Purifying Light")!!
                val abilityId = cardRegistry.getCard("Apostle of Purifying Light")!!.script.activatedAbilities[0].id
                val giant = game.findCardsInGraveyard(2, "Hill Giant").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                val first = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = apostle,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(giant, game.player2Id, Zone.GRAVEYARD)),
                    )
                )
                withClue("first activation should succeed: ${first.error}") { first.error shouldBe null }
                game.resolveStack()

                withClue("the opponent's card is exiled") {
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.graveyardSize(2) shouldBe 0
                }

                val second = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = apostle,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                    )
                )
                withClue("second activation should succeed: ${second.error}") { second.error shouldBe null }
                game.resolveStack()

                withClue("its controller's own card is exiled too") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.graveyardSize(1) shouldBe 0
                }
            }
        }
    }
}
