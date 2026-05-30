package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Hanna, Ship's Navigator.
 *
 * {1}{W}{U} Legendary Creature — Human Artificer 1/2
 * "{1}{W}{U}, {T}: Return target artifact or enchantment card from your graveyard to your hand."
 */
class HannaShipsNavigatorScenarioTest : ScenarioTestBase() {

    init {
        context("Hanna returns an artifact or enchantment from the graveyard") {
            test("returns an artifact card from your graveyard to your hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hanna, Ship's Navigator")
                    .withCardInGraveyard(1, "Tigereye Cameo")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Pay {1}{W}{U}.
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(ManaPoolComponent(white = 1, blue = 1, colorless = 1))
                }

                val hanna = game.findPermanent("Hanna, Ship's Navigator")!!
                val cameo = game.state.getZone(game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
                    .first { game.state.getEntity(it)
                        ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Tigereye Cameo" }

                withClue("Tigereye Cameo starts in the graveyard") {
                    game.isInGraveyard(1, "Tigereye Cameo") shouldBe true
                }

                val ability = cardRegistry.getCard("Hanna, Ship's Navigator")!!.script.activatedAbilities[0]
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hanna,
                        abilityId = ability.id,
                        targets = listOf(
                            ChosenTarget.Card(cameo, game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
                        )
                    )
                )
                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Tigereye Cameo should now be in hand") {
                    game.isInHand(1, "Tigereye Cameo") shouldBe true
                }
                withClue("Tigereye Cameo should no longer be in the graveyard") {
                    game.isInGraveyard(1, "Tigereye Cameo") shouldBe false
                }
            }
        }
    }
}
