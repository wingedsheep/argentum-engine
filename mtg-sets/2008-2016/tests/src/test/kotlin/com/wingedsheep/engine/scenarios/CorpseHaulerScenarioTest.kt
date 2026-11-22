package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m14.cards.CorpseHauler
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Corpse Hauler (M14 #90) — "{2}{B}, Sacrifice this creature: Return target creature card from
 * your graveyard to your hand."
 */
class CorpseHaulerScenarioTest : ScenarioTestBase() {

    init {
        context("Corpse Hauler") {
            test("sacrificing it returns a creature card from your graveyard to your hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Corpse Hauler")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Corpse Hauler")!!,
                        abilityId = CorpseHauler.activatedAbilities.first().id,
                        targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                    )
                ).error shouldBe null

                withClue("the Hauler is sacrificed as a cost") {
                    game.isInGraveyard(1, "Corpse Hauler") shouldBe true
                }
                game.resolveStack()

                withClue("the targeted creature card is back in hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
