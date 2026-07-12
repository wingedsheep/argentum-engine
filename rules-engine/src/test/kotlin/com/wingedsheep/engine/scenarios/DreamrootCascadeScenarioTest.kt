package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Dreamroot Cascade (VOW #262) — Land.
 *
 *   This land enters tapped unless you control two or more other lands.
 *   {T}: Add {G} or {U}.
 *
 * One of the "slow lands": enters untapped only with two or more other lands already in play
 * (i.e. three or more lands total, counting itself). Exercises both the tapped-entry replacement
 * and the two-mode {T} mana ability.
 */
class DreamrootCascadeScenarioTest : ScenarioTestBase() {

    init {
        context("Dreamroot Cascade — enters tapped unless you control two or more other lands") {

            test("enters tapped as the very first land") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dreamroot Cascade")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cascade = game.findCardsInHand(1, "Dreamroot Cascade").first()
                game.execute(PlayLand(game.player1Id, cascade)).error shouldBe null

                withClue("With no other lands, Dreamroot Cascade enters tapped") {
                    game.state.getEntity(cascade)?.has<TappedComponent>() shouldBe true
                }
            }

            test("enters untapped with two or more other lands already controlled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dreamroot Cascade")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cascade = game.findCardsInHand(1, "Dreamroot Cascade").first()
                game.execute(PlayLand(game.player1Id, cascade)).error shouldBe null

                withClue("With two other lands, Dreamroot Cascade enters untapped") {
                    game.state.getEntity(cascade)?.has<TappedComponent>() shouldBe false
                }
            }

            test("{T}: Add {G}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dreamroot Cascade", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cascade = game.findPermanent("Dreamroot Cascade")!!
                val greenAbility = cardRegistry.getCard("Dreamroot Cascade")!!.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = cascade, abilityId = greenAbility)
                ).error shouldBe null

                withClue("taps for green") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.green shouldBe 1
                }
            }

            test("{T}: Add {U} on a second copy") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dreamroot Cascade", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cascade = game.findPermanent("Dreamroot Cascade")!!
                val blueAbility = cardRegistry.getCard("Dreamroot Cascade")!!.activatedAbilities[1].id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = cascade, abilityId = blueAbility)
                ).error shouldBe null

                withClue("taps for blue") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.blue shouldBe 1
                }
            }
        }
    }
}
