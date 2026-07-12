package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Honored Heirloom (VOW #257) — {3} Artifact.
 *
 *   {T}: Add one mana of any color.
 *   {2}, {T}: Exile target card from a graveyard.
 *
 * Exercises both activated abilities: tapping for a chosen color of mana, and (on a fresh copy,
 * since the first ability taps the permanent) paying {2} and tapping to exile a target card from
 * a graveyard.
 */
class HonoredHeirloomScenarioTest : ScenarioTestBase() {

    init {
        context("Honored Heirloom") {

            test("{T}: Add one mana of any color") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Honored Heirloom", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heirloom = game.findPermanent("Honored Heirloom")!!
                val manaAbilityId = cardRegistry.getCard("Honored Heirloom")!!.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = heirloom,
                        abilityId = manaAbilityId,
                        manaColorChoice = Color.BLACK
                    )
                ).error shouldBe null

                withClue("taps for one black mana") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.black shouldBe 1
                }
            }

            test("{2}, {T}: Exile target card from a graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Honored Heirloom", tapped = false)
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heirloom = game.findPermanent("Honored Heirloom")!!
                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").first()
                val exileAbilityId = cardRegistry.getCard("Honored Heirloom")!!.activatedAbilities[1].id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = heirloom,
                        abilityId = exileAbilityId,
                        targets = listOf(ChosenTarget.Card(bears, game.player2Id, Zone.GRAVEYARD))
                    )
                )
                withClue("Activating the exile ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears is exiled from the graveyard") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
