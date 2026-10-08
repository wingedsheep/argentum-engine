package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Clay Revenant (BRO #118) — {1} Artifact Creature — Golem, 1/2.
 *
 * "This creature enters tapped. {2}{B}: Return this card from your graveyard to your hand."
 */
class ClayRevenantScenarioTest : ScenarioTestBase() {

    init {
        context("Clay Revenant") {

            test("enters the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Clay Revenant")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Clay Revenant").error shouldBe null
                game.resolveStack()

                val revenant = game.findPermanent("Clay Revenant")!!
                withClue("Clay Revenant enters tapped") {
                    game.state.getEntity(revenant)!!.has<TappedComponent>() shouldBe true
                }
            }

            test("{2}{B} returns it from the graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Clay Revenant")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val revenant = game.findCardsInGraveyard(1, "Clay Revenant").single()
                val abilityId = cardRegistry.getCard("Clay Revenant")!!.script.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = revenant,
                        abilityId = abilityId,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Clay Revenant") shouldBe true
                game.isInGraveyard(1, "Clay Revenant") shouldBe false
            }
        }
    }
}
